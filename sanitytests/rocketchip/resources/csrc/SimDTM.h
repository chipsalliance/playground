#include <fesvr/dtm.h>

class ledtm_t : public dtm_t
{
  public:
    ledtm_t(int argc, char**argv);
    ~ledtm_t();
    endianness_t get_target_endianness() const;
};
