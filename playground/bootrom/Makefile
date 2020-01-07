DBAUD_DIV ?= 260
RESET_ADDR ?= 0x10000
UART_ADDR ?= 0x10012000
CPPFLAGS += -DBAUD_DIV=$(BAUD_DIV) -DRESET_ADDR=$(RESET_ADDR) -DUART_ADDR=$(UART_ADDR)
CFLAGS += -march=rv64g -fPIC --shared -nostdlib -O2
CC=riscv64-linux-gnu-gcc

boot.bin: boot.gen.lds boot.o
	riscv64-linux-gnu-ld -T $? -o tmp.bin
	riscv64-linux-gnu-objcopy -O binary tmp.bin $@
	rm tmp.bin
boot.o: boot.c common.h uart.h
%.gen.lds: %.lds
	$(CC) -E -x c $(CFLAGS) $(CPPFLAGS) $? | grep -v '^#' >>$@
