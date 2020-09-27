#ifndef STREAM_PROGRAM_HPP
#define STREAM_PROGRAM_HPP

namespace dbtoaster {
  class StreamProgram : public Program {
  public:
    StreamProgram(int argc=0, char *argv[] = nullptr): Program(argc, argv) {};
    void init();
    void process_streams();

  private:
    void estimate_tuple_count();
  };
}

#endif
