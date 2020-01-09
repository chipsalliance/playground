SECTIONS
{
	.boot RESET_ADDR : {
    uart0 = UART_ADDR;
    *(.entry)
    *(.text)
    }
}
