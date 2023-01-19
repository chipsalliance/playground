SECTIONS
{
    ROM_BASE = 0x10000; /* ... but actually position independent */

    . = ROM_BASE;
    .rodata : { *(.rodata) }
    . = ROM_BASE + 0x40;
    .text.hang : { *(.text.hang) }
    .text : { *(.text) }
}
