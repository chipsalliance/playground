#include "common.h"
#include <stdint.h>
#include <stdatomic.h>

typedef _Atomic(uint32_t) a_uint32_t;
typedef struct {
    a_uint32_t txdata, rxdata;
    a_uint32_t txctrl, rxctrl;
    a_uint32_t ie, ip;
    a_uint32_t div;
} uart_mmp;

extern uart_mmp uart0;

int init_uart() {
	atomic_store(&uart0.txctrl, 0b11);
	atomic_store(&uart0.div, BAUD_DIV - 1);
	return 0;
}

void sb_putchar(char c_) {
	int c = c_;
	while (atomic_exchange(&uart0.txdata, c)) {
		continue;
	}
}

/*
int sb_getchar() {
	return -1;
}
*/

void sb_puts(const char* str) {
	char c;
	while (0 != (c = *(str++))) {
		sb_putchar(c);
	}
}
