/* -*- Mode: C; indent-tabs-mode: t; c-basic-offset: 4; tab-width: 4 -*- */
#include <csignal>
#include <sys/mman.h>
#include "StreamProgram.hpp"

dbtoaster::IProgram *gp;
void finish_measurement(int signum) {
    gp->print_log_buffer();
    exit(signum);
}

// Simple DBToaster driver that feeds events sequentially after mapping the complete
// data set into memory.
// TODO: print log buffer into stream, and direct this into a file (or cout, if
// nothing specified)
int main(int argc, char* argv[]) {
#ifdef __linux__ || __APPLE__
	if (mlockall(MCL_CURRENT | MCL_FUTURE)) {
		cerr << "Could not lock memory, exiting" << endl;
		cerr << "(did you forget to set cap_ipc_lock?)" << endl;
		cerr << "Continuing w/o locked memory" << endl;
  }
#endif

	signal(SIGINT, finish_measurement);
	dbtoaster::StreamProgram p(argc,argv);
    dbtoaster::Program::snapshot_t snap;
	gp = &p;

    p.init();
    p.run(false);

	p.print_log_buffer();
	return 0;
}
