SECTIONS
{
	.boot RESET_ADDR : {
    *(.entry)
    *(.text)
    }
    uart0 = UART_ADDR;
}
