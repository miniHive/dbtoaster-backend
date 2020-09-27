#ifndef DBTOASTER_STREAMS_H
#define DBTOASTER_STREAMS_H

#include <memory>
#include <map>
#include <set>
#include <list>
#include <vector>
#include <queue>
#include <string>
#include <fstream>
#include <streambuf>
#include <sys/time.h>

#include <functional>

#include "runtime.hpp"
#include "event.hpp"


namespace dbtoaster {
namespace streams {

// Adaptor and stream interfaces.

struct stream_adaptor
{
    stream_adaptor() {}

    virtual void read_adaptor_events(char* data, std::shared_ptr<std::list<event_t> > eventList, std::shared_ptr<std::list<event_t> > eventQue) = 0;
};

// Framing
enum frame_type { fixed_size, delimited, variable_size };
struct frame_descriptor {
    frame_type type;
    int size;
    std::string delimiter;
    int off_to_size;
    int off_to_end;
    frame_descriptor() : type(delimited), size(0), delimiter("\n") {}
    frame_descriptor(std::string d) : type(delimited), size(0), delimiter(d) {}
    frame_descriptor(int sz) : type(fixed_size), size(sz) {}
    frame_descriptor(int os, int oe)
    : type(variable_size), size(0), off_to_size(os), off_to_end(oe)
    {}
};

// Sources
struct source
{
    frame_descriptor frame_info;
    std::shared_ptr<stream_adaptor> adaptor;

    source(frame_descriptor& f, std::shared_ptr<stream_adaptor> a);
    
    // Process adaptors in the first stage, accumulating and returning
    // stream events
    virtual void read_source_events(std::shared_ptr<std::list<event_t> > eventList, std::shared_ptr<std::list<event_t> > eventQue) = 0;

    virtual void init_source() = 0;
};

struct dbt_file_source : public source
{
    char *data = nullptr;
    size_t size;

    dbt_file_source(const std::string& path, frame_descriptor& f, std::shared_ptr<stream_adaptor> a);

    void read_source_events(std::shared_ptr<std::list<event_t> > eventList, std::shared_ptr<std::list<event_t> > eventQue);

    void init_source() {}
};

struct source_multiplexer
{
    std::vector<std::shared_ptr<source> > inputs;
    std::shared_ptr<source> current;
    int step, remaining, block;
    std::shared_ptr<std::list<event_t> > eventList;
    std::shared_ptr<std::list<event_t> > eventQue;

    source_multiplexer(int seed, int st);
    source_multiplexer(int seed, int st, std::set<std::shared_ptr<source> >& s);

    void add_source(std::shared_ptr<source> s);
    void remove_source(std::shared_ptr<source> s);

    void init_source(size_t batch_size, size_t parallel, bool is_table);
};

}
}

#endif

