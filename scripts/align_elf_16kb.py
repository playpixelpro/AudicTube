#!/usr/bin/env python3
"""
ELF 16KB Page Alignment Fixer
Post-processes arm64-v8a .so files so PT_LOAD & PT_GNU_RELRO segments
align to 16KB (0x4000). Idempotent (multi-pass until convergence).
Usage: python align_elf_16kb.py <path.so> [--verbose]
"""
import os, struct, sys

ELFCLASS64 = 2; PT_LOAD = 1; PT_GNU_RELRO = 0x6474e552
TARGET_ALIGN = 0x4000

class Elf64Ehdr:
    fmt = '<16sHHIQQQIHHHHHH'
    def __init__(self, data):
        f = struct.unpack_from(self.fmt, data, 0)
        (self.e_ident, self.e_type, self.e_machine, self.e_version,
         self.e_entry, self.e_phoff, self.e_shoff, self.e_flags,
         self.e_ehsize, self.e_phentsize, self.e_phnum,
         self.e_shentsize, self.e_shnum, self.e_shstrndx) = f

class Elf64Phdr:
    fmt = '<IIQQQQQQ'
    def __init__(self, data, offset):
        self.raw_offset = offset
        f = struct.unpack_from(self.fmt, data, offset)
        (self.p_type, self.p_flags, self.p_offset, self.p_vaddr,
         self.p_paddr, self.p_filesz, self.p_memsz, self.p_align) = f

def read_elf(path):
    with open(path, 'rb') as f:
        return bytearray(f.read())

def write_elf(path, data):
    with open(path, 'wb') as f:
        f.write(data)

def parse_phdrs(data):
    ehdr = Elf64Ehdr(data)
    entries = []
    for i in range(ehdr.e_phnum):
        po = ehdr.e_phoff + i * ehdr.e_phentsize
        ph = Elf64Phdr(data, po)
        entries.append({'idx': i, 'raw_offset': po, 'p_type': ph.p_type,
            'p_flags': ph.p_flags, 'p_offset': ph.p_offset,
            'p_vaddr': ph.p_vaddr, 'p_paddr': ph.p_paddr,
            'p_filesz': ph.p_filesz, 'p_memsz': ph.p_memsz,
            'p_align': ph.p_align})
    return ehdr, entries

def pack_ehdr(e):
    return struct.pack(e.fmt, e.e_ident, e.e_type, e.e_machine,
        e.e_version, e.e_entry, e.e_phoff, e.e_shoff, e.e_flags,
        e.e_ehsize, e.e_phentsize, e.e_phnum,
        e.e_shentsize, e.e_shnum, e.e_shstrndx)

def pack_phdr(e):
    return struct.pack('<IIQQQQQQ', e['p_type'], e['p_flags'],
        e['p_offset'], e['p_vaddr'], e['p_paddr'],
        e['p_filesz'], e['p_memsz'], e['p_align'])


def fix_elf_align(input_path, verbose=False):
    for pass_num in range(1, 101):
        data = read_elf(input_path)
        orig_size = len(data)
        if data[:4] != b'\x7fELF' or data[4] != ELFCLASS64:
            print("ERROR: Not 64-bit ELF: %s" % input_path); return False
        ehdr, ents = parse_phdrs(data)

        # Phase A: PT_LOAD padding
        inserts = []
        for ent in ents:
            if ent['p_type'] != PT_LOAD: continue
            need = (ent['p_vaddr'] - ent['p_offset']) % TARGET_ALIGN
            if need > 0:
                inserts.append((ent['p_offset'], need))
                if verbose:
                    print("  LOAD[%d]: 0x%x->0x%x (pad=%d, align=0x%x->0x%x)" %
                          (ent['idx'], ent['p_offset'], ent['p_offset']+need,
                           need, ent['p_align'], TARGET_ALIGN))

        if inserts:
            inserts.sort(key=lambda x: x[0], reverse=True)
            cum = 0
            adj = bytearray(data)
            for ia, pad in inserts:
                adj[(ia+cum):(ia+cum)] = b'\x00' * pad
                cum += pad
            data = adj  # <-- FIX: use the padded data from now on
            for ent in ents:
                off = ent['p_offset']
                for ia, pad in inserts:
                    if ent['p_offset'] >= ia: off += pad
                ent['p_offset'] = off
                if ent['p_type'] == PT_LOAD: ent['p_align'] = TARGET_ALIGN
                raw = ent['raw_offset']
                for ia, pad in inserts:
                    if ent['raw_offset'] >= ia: raw += pad
                ent['raw_offset'] = raw
            for field in ('e_phoff', 'e_shoff'):
                v = getattr(ehdr, field)
                if v > 0:
                    for ia, pad in inserts:
                        if v >= ia: v += pad
                    setattr(ehdr, field, v)
            data[0:64] = pack_ehdr(ehdr)
            for ent in ents:
                data[ent['raw_offset']:ent['raw_offset']+56] = pack_phdr(ent)
            write_elf(input_path, data)
            if verbose:
                print("  Pass %d: wrote %d bytes (+%d)" %
                      (pass_num, len(data), len(data)-orig_size))
            continue

        # Phase B: PT_GNU_RELRO alignment
        relro = False
        for ent in ents:
            if ent['p_type'] != PT_GNU_RELRO: continue
            s = ent['p_vaddr']; e = s + ent['p_memsz']
            ns = s & ~(TARGET_ALIGN - 1)
            ne = (e + TARGET_ALIGN - 1) & ~(TARGET_ALIGN - 1)
            if ns != s or ne != e:
                if verbose:
                    print("  RELRO[%d]: 0x%x-0x%x -> 0x%x-0x%x" %
                          (ent['idx'], s, e, ns, ne))
                ent['p_vaddr'] = ns; ent['p_memsz'] = ne - ns
                data[ent['raw_offset']:ent['raw_offset']+56] = pack_phdr(ent)
                write_elf(input_path, data)
                relro = True
                if verbose:
                    print("  Pass %d: RELRO extended" % pass_num)
        if relro: continue

        # Done — no changes needed this pass
        if verbose and pass_num == 1:
            print("Already aligned: %s" % input_path)
        return True
    print("ERROR: max passes: %s" % input_path)
    return False


def main():
    if len(sys.argv) < 2:
        print("Usage: python align_elf_16kb.py <path-to-so> [--verbose]"); sys.exit(1)
    fp = sys.argv[1]; vb = '--verbose' in sys.argv or '-v' in sys.argv
    if not os.path.isfile(fp):
        print("ERROR: Not found: %s" % fp); sys.exit(1)
    if vb: print("Processing: %s" % fp)
    with open(fp, 'rb') as f: magic = f.read(4)
    if magic == b'\x7fELF':
        ok = fix_elf_align(fp, vb)
    else:
        print("WARNING: Not ELF (magic=0x%s), skipped: %s" % (magic.hex(), fp)); ok = True
    sys.exit(0 if ok else 1)

if __name__ == '__main__':
    main()