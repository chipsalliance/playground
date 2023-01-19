/* Macro definition from freedom-metal i2c driver */
/* Copyright 2019 SiFive, Inc */
/* SPDX-License-Identifier: Apache-2.0 */

/* Register fields */
#define METAL_I2C_CONTROL_EN (1UL << 7)
#define METAL_I2C_CONTROL_IE (1UL << 6)
#define METAL_I2C_WRITE (0UL << 0)
#define METAL_I2C_READ (1UL << 0)
#define METAL_I2C_CMD_START (1UL << 7)
#define METAL_I2C_CMD_STOP (1UL << 6)
#define METAL_I2C_CMD_READ (1UL << 5)
#define METAL_I2C_CMD_WRITE (1UL << 4)
#define METAL_I2C_CMD_ACK (1UL << 3)
#define METAL_I2C_CMD_IACK (1UL << 0)
#define METAL_I2C_STATUS_RXACK (1UL << 7)
#define METAL_I2C_STATUS_BUSY (1UL << 6)
#define METAL_I2C_STATUS_AL (1UL << 5)
#define METAL_I2C_STATUS_TIP (1UL << 1)
#define METAL_I2C_STATUS_IP (1UL << 0)

/* Prescaler max value */
#define METAL_I2C_PRESCALE_MAX 0xFFFF

/* Check endianess */
#if __BYTE_ORDER__ != __ORDER_LITTLE_ENDIAN__
#error *** Unsupported endianess ***
#endif

#define METAL_SIFIVE_I2C_INSERT_STOP(stop_flag) ((stop_flag & 0x01UL) << 6)
#define METAL_SIFIVE_I2C_INSERT_RW_BIT(addr, rw)                               \
    ((addr & 0x7FUL) << 1 | (rw & 0x01UL))
#define METAL_SIFIVE_I2C_GET_PRESCALER(baud, clock_rate)                       \
    ((clock_rate / (baud * 5)) - 1)
