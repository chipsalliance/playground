#include "common.hpp"

typedef struct {
    sb_uint32_t txdata, rxdata;
    sb_uint32_t txctrl, rxctrl;
    sb_uint32_t ie, ip;
    sb_uint32_t div;
} uart_mmp;

extern uart_mmp uart0;

int init_uart() {
	uart0.txctrl.store(0b11);
	uart0.div.store(BAUD_DIV - 1);
	return 0;
}

void sb_putchar(char c) {
    while (uart0.txdata.load()) {
        continue;
    }
    uart0.txdata.store(c);
}

void sb_puts(const char* str) {
	char c;
	while (0 != (c = *(str++))) {
		sb_putchar(c);
	}
}
