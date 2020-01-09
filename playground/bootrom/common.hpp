#pragma once
#include <cstdint>

#ifdef USE_ATOMIC
#include <atomic>
struct sb_uint32_t {
    std::atomic<uin32_t> v;
    uint32_t load() {
        return v.load();
    }
    void store(uint32_t v_) {
        v.store(v_);
    }
};
#else
struct sb_uint32_t {
    volatile uint32_t v;
    uint32_t load() {
        return v;
    }
    void store(uint32_t v_) {
        v = v_;
    }
};
#endif

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

