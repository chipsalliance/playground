#include "common.h"

struct gpio {
    sb_uint32_t input_val, input_en;
    sb_uint32_t outoput_en, output_val;
    sb_uint32_t pue, ds;
    sb_uint32_t rise_ie, rise_ip;
    sb_uint32_t fall_ie, fall_ip;
    sb_uint32_t high_ie, high_ip;
    sb_uint32_t low_ie, low_ip;
    sb_uint32_t out_xor;
};
