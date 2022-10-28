// assumes unsigned long === uint64_t
volatile unsigned long tohost __attribute__ ((section (".htif")));
volatile unsigned long fromhost __attribute__ ((section (".htif")));

static void do_tohost(unsigned long tohost_value)
{
  while (tohost)
    fromhost = 0;
  tohost = tohost_value;
}

static void cputchar(int x)
{
  do_tohost(0x0101000000000000UL | (unsigned char)x);
}

static void cputstring(const char* s)
{
  while (*s)
    cputchar(*s++);
}

static void terminate(int code)
{
  do_tohost(code);
  while (1);
}

int main(void)
{
  cputstring("Hello World!\n");
  terminate(1);
}
