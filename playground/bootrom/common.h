#pragma once

#define CSRR(csr) \
({              \
  int dst;      \
  asm volatile ("csrr %0, " #csr "\n\t"  \
    : "=r" (dst)  \
    :); \
  dst; \
})

#define CSRW(x, v) \
({              \
  int src = (v);      \
  asm volatile ("csrw %0, " #x "\n\t"  \
    :  \
    : "r"(src)); \
})

static inline int get_hart_id() {
	return CSRR(mhartid);
}

