    .section .text._entry
    .global _entry
_entry:
    la sp, _stack_end
    j main
