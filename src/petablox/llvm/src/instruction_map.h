//#ifndef INSTRUCTIONS_MAP_H
//#define INSTRUCTIONS_MAP_H
#include <map>
#include <set>

using namespace std;

extern map<unsigned long, int> instruction_ids;
extern set<unsigned long> instructions;

extern map<unsigned long, int> basicblock_ids;
extern set<unsigned long> basicblocks;

//#endif
