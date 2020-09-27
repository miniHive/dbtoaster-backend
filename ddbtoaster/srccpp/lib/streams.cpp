#include "streams.hpp"

#include "runtime.hpp"

#include "filepath.hpp"

#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>

using namespace ::dbtoaster::runtime;

namespace dbtoaster {
namespace streams {

/******************************************************************************
	source
******************************************************************************/
source::source(frame_descriptor& f, std::shared_ptr<stream_adaptor> a) : frame_info(f), adaptor(a) {
}

/******************************************************************************
	dbt_file_source
******************************************************************************/
dbt_file_source::dbt_file_source(
		const std::string& path, frame_descriptor& f, std::shared_ptr<stream_adaptor> a): source(f,a)
{
	if ( file_exists( path ) )
	{
		int fd = open(path.c_str(), O_RDONLY);
		size = lseek(fd, 0, SEEK_END);

		#ifdef __linux__
		mmap(data, size, PROT_READ|PROT_WRITE, MAP_FILE|MAP_PRIVATE|MAP_POPULATE, fd, 0);
		#else
		mmap(data, size, PROT_READ|PROT_WRITE, MAP_FILE|MAP_PRIVATE, fd, 0);
	        #endif
		if (!data) {
			std::cerr << "Internal error: mmap of existing file failed" << std::endl;
			exit(-1);
		}
	  	if( runtime_options::verbose() )
			std::cerr << "reading from " << path
				 << " with 1 adaptors" << std::endl;
	}
	else
		std::cerr << "File not found: " << path << std::endl;
}

void dbt_file_source::read_source_events(std::shared_ptr<std::list<event_t> > eventList, std::shared_ptr<std::list<event_t> > eventQue) {
	char* buffer = data;
	char* buffer_end = data + size;

	char* start_event_pos = buffer;
	char* end_event_pos = buffer;
	if (frame_info.type == fixed_size) {
		size_t frame_size = frame_info.size;
		char tmp;
		for(; start_event_pos != buffer_end; start_event_pos = end_event_pos) {
			end_event_pos = start_event_pos+frame_size;
			tmp = *end_event_pos;
			*end_event_pos = '\0';
			adaptor->read_adaptor_events(start_event_pos,eventList,eventQue);
			*end_event_pos = tmp;
		}
	}
	else if ( frame_info.type == delimited ) {
		const char* delim = frame_info.delimiter.c_str();
		size_t delim_size = frame_info.delimiter.size();

		// Check if delimiter is present at the end
		for(size_t delim_idx = 0; delim_idx < delim_size; ++delim_idx) {
			if(*(buffer_end-1-delim_idx) != *(delim+delim_size-1)) {
				std::cerr << "Wrong delimiter at EOF, go fix your data " << std::endl;
				exit(-1);
			}
		}

		while(start_event_pos) {
			end_event_pos = strstr(start_event_pos, delim);
			if(!end_event_pos || end_event_pos == buffer_end) break;
			*end_event_pos = '\0';
			adaptor->read_adaptor_events(start_event_pos,eventList,eventQue);
			start_event_pos = end_event_pos + delim_size;
		}
	} else if ( frame_info.type == variable_size ) {
		std::cerr << "variable size frames not supported" << std::endl;
	} else {
		std::cerr << "invalid frame type" << std::endl;
	}
	delete[] buffer;
}
/******************************************************************************
	source_multiplexer
******************************************************************************/
source_multiplexer::source_multiplexer(int seed, int st)
	: step(st), remaining(0), block(100)
{
	srand(seed);
	eventList = std::shared_ptr<std::list<event_t> >(new std::list<event_t>());
	eventQue = std::shared_ptr<std::list<event_t> >(new std::list<event_t>());
}

source_multiplexer::source_multiplexer(int seed, int st, 
										std::set<std::shared_ptr<source> >& s)
{
	source_multiplexer(seed, st);
	std::set<std::shared_ptr<source> >::iterator it = s.begin();
	std::set<std::shared_ptr<source> >::iterator end = s.end();
	for(; it != end; ++it) add_source(*it);
}

void source_multiplexer::add_source(std::shared_ptr<source> s) {
	inputs.push_back(s);
}

void source_multiplexer::remove_source(std::shared_ptr<source> s) {
	std::vector<std::shared_ptr<source> >::iterator end = inputs.end();
	for (std::vector<std::shared_ptr<source> >::iterator it = inputs.begin(); it != end; ++it) {
		if((*it) == s) inputs.erase(it);
	}
}

void source_multiplexer::init_source(size_t batch_size, size_t parallel, bool is_table) {
	std::vector<std::shared_ptr<source> >::iterator it = inputs.begin();
	std::vector<std::shared_ptr<source> >::iterator end = inputs.end();
	for (; it != end; ++it) {
		std::shared_ptr<source> s = (*it);
		if(s) {
			s->init_source();
			s->read_source_events(eventList, eventQue);
		}
	}
	if(batch_size > 1) {
		std::list<event_t> batchedEventList;
		map<relation_id_t,std::vector<event_t*> > tuples_queued_in_relations;


		if(!eventList->empty()) {
			std::list<event_t>::iterator eit = eventList->begin();
			std::list<event_t>::iterator eit_end = eventList->end();
			for(;eit != eit_end; ++eit) {
				event_t* evt = &(*eit);
				tuples_queued_in_relations[evt->id].push_back(evt);
			}
		}
		if(!eventQue->empty()) {
			eventQue->sort(compare_event_timestamp_order);
			std::list<event_t>::iterator eit = eventQue->begin();
			std::list<event_t>::iterator eit_end = eventQue->end();
			event_args_t batch;

			for(;eit != eit_end;) {
				event_t* evt = &(*eit);
				event_args_t* evtData = new event_args_t(evt->data);
				if(evt->type == insert_tuple) evtData->push_back(std::shared_ptr<long>(new long( 1L)));
				else evtData->push_back(std::shared_ptr<long>(new long(-1L)));

				// add relation to last element
				evtData->push_back(std::shared_ptr<int>(new int(evt->id)));

				batch.push_back(std::shared_ptr<event_args_t>(evtData));
				// increment iterator
				++eit;
				if(batch.size() >= batch_size || eit == eit_end) {
					event_t e(batch_update, evt->id, evt->event_order, batch);
					batchedEventList.push_back(e);
					batch.clear();
				}
			}
		}
		if (!eventList->empty()) {
			map<relation_id_t, std::vector<event_t*> >::iterator it = tuples_queued_in_relations.begin();
			map<relation_id_t, std::vector<event_t*> >::iterator it_end = tuples_queued_in_relations.end();
			event_args_t batch;
			for(; it != it_end; ++it) {
				while(!it->second.empty()) {
					event_t* evt = it->second.back();
					it->second.pop_back();
					event_args_t* evtData = new event_args_t(evt->data);
					if(evt->type == insert_tuple) evtData->push_back(std::shared_ptr<long>(new long( 1L)));
					else evtData->push_back(std::shared_ptr<long>(new long(-1L)));

					// add relation to last element
					evtData->push_back(std::shared_ptr<int>(new int(evt->id)));
					batch.push_back(std::shared_ptr<event_args_t>(evtData));
					if(batch.size() >= batch_size || it->second.empty()) {
						event_t e(batch_update, evt->id, evt->event_order, batch);
						batchedEventList.push_back(e);
						batch.clear();
					}
				}
			}
		}
		if(eventQue->empty()) {
			eventList->clear();
			eventList->insert(eventList->end(), batchedEventList.begin(), batchedEventList.end());
		} else {
			eventList->clear();
			eventQue->clear();
			eventQue->insert(eventQue->end(), batchedEventList.begin(), batchedEventList.end());
		}
	}
	size_t num_relations = inputs.size();
	if(!is_table && !eventList->empty() && parallel == MIX_INPUT_TUPLES && num_relations > 1) { //we do not interleave table tuples as they are part of preprocessing and we do not even time their calculations
		std::list<event_t>::reverse_iterator it = eventList->rbegin();
		std::list<event_t>::reverse_iterator it_end = eventList->rend();
		std::vector<event_t> events_by_relation[num_relations];
		for(;it != it_end; ++it) {
			events_by_relation[it->id].push_back(*it);
		}
		eventList->clear();
		bool thereAreMoreTuples = true;
		while(thereAreMoreTuples) {
			thereAreMoreTuples = false;
			for(size_t i = 0; i < num_relations; ++i) {
				if(events_by_relation[i].size() > 0) {
					thereAreMoreTuples = true;
					eventList->push_back(events_by_relation[i].back());
					events_by_relation[i].pop_back();
				}
			}
		}
	}
	if(!eventQue->empty()) {
		eventQue->sort(compare_event_timestamp_order);
	}
}

}

}
