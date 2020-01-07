#include <stdatomic.h>

#include "common.h"
#include "uart.h"

#define SLOGAN "Good bye, ugly world!"

static atomic_int init_finish = 0;

void __attribute__((noreturn)) __attribute__ ((section (".entry"))) entry() {
	int hartid = get_hart_id();
	if (hartid == 0) {
		init_uart();
		atomic_store(&init_finish, 1);
		while (1) {
			sb_puts(SLOGAN);
		}
	} else {
		while (!atomic_load(&init_finish)) {
			continue;
		}
	}
	__builtin_unreachable();
}

