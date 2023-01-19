#include "i2c.h"

static char volatile * const I2C_PRESCALER_LO = (char *)0x10002000;
static char volatile * const I2C_PRESCALER_HI = (char *)0x10002004;
static char volatile * const I2C_CTRL = (char *)0x10002008;
static char volatile * const I2C_DATA = (char *)0x1000200C;
static char volatile * const I2C_STAT = (char *)0x10002010;
static char volatile * const I2C_CMD  = (char *)0x10002010;

// These two I2C registers are set for correct Vref for FMC2 IO
static char const __attribute__((section(".rodata"))) i2c_reg[] = {0x06, 0xfc, 0x07, 0xfc};

void i2c_write(char const * buf, unsigned int size);

__attribute__((naked, section(".text.hang")))
void _hang(void) {
    __asm__ __volatile__ (
        "li sp, 0xFFFFFF00\n\t"
        "jal _start\n\t"
    );
}

void _start(void) {
    // Program the FMC voltage
    long const prescaler = METAL_SIFIVE_I2C_GET_PRESCALER(200000, 100000000);
    *I2C_PRESCALER_LO = (char)prescaler;
    *I2C_PRESCALER_HI = (char)(prescaler >> 8);

    *I2C_CTRL |= METAL_I2C_CONTROL_EN;

    i2c_write(i2c_reg, 2);
    i2c_write(i2c_reg + 2, 2);

    while(1);
}

void i2c_write(char const * buf, unsigned int size) {
    while((*I2C_STAT) & METAL_I2C_STATUS_TIP);
    *I2C_DATA = METAL_SIFIVE_I2C_INSERT_RW_BIT(0xC2, METAL_I2C_WRITE);
    *I2C_CMD = METAL_I2C_CMD_WRITE | METAL_I2C_CMD_START;
    while((*I2C_STAT) & METAL_I2C_STATUS_TIP);
    if ((*I2C_STAT) & METAL_I2C_STATUS_RXACK) { while(1); }

    for (int i = 0; i < size; i++) {
        char cmd = METAL_I2C_CMD_WRITE;
        *I2C_DATA = buf[i];
        if (i == (size - 1)) {
            cmd |= METAL_I2C_CMD_STOP;
        }
        *I2C_CMD = cmd;
        while((*I2C_STAT) & METAL_I2C_STATUS_TIP);
        if ((*I2C_STAT) & METAL_I2C_STATUS_RXACK) { while(1); }
    }
}
