OUTPUT_ARCH ("riscv")
ENTRY (_entry)

SECTIONS
{
    . = 0x80000000;

    .text : {
        *(.text._entry)
        *(.text .text.*)
    }

    .rodata : {
        *(.rodata .rodata.*)
        *(.gnu.linkonce.r.*)
    }

    /* Host-Target Interface */
    .htif ALIGN(0x40) : {
        *(.htif)
    }

    .data : ALIGN (8) {
        *(.data)
        *(.gnu.linkonce.d.*)
    }

    . = ALIGN(0x1000);
    _stack_start = .;
    . += 1024;
    _stack_end = .;
}
