BAUD_DIV ?= 260
RESET_ADDR ?= 0x10040
UART_ADDR ?= 0x10012000
CPPFLAGS += -DBAUD_DIV=$(BAUD_DIV) -DRESET_ADDR=$(RESET_ADDR) -DUART_ADDR=$(UART_ADDR)
CFLAGS += -march=rv64g -nostdlib -O2 -static -ggdb3
CXXFLAGS += $(CFLAGS)
CC=riscv64-linux-gnu-gcc
CXX=riscv64-linux-gnu-g++

boot.bin: boot.elf
	riscv64-linux-gnu-objcopy -O binary $? $@
boot.elf: boot.gen.lds boot.o
	riscv64-linux-gnu-ld -T $? -o $@
boot.o: boot.cpp common.hpp uart.hpp
%.gen.lds: %.lds
	$(CC) -E -x c $(CFLAGS) $(CPPFLAGS) $? | grep -v '^#' >>$@
clean:
	rm -f *.gen.lds *.o *.out *.bin *.elf
