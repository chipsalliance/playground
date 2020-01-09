#include "common.hpp"
#include "uart.hpp"

#define SLOGAN "Good bye, ugly world!"

sb_uint32_t init_finish;

void __attribute__((noreturn)) __attribute__ ((section (".entry"))) entry() {
	int hartid = get_hart_id();
	if (hartid == 0) {
		init_uart();
        init_finish.store(1);
        while (1) {
		    sb_puts(SLOGAN);
        }
	} else {
		while (!init_finish.load()) {
			continue;
		}
        while (true) {
            continue;
        }
	}
	__builtin_unreachable();
}

