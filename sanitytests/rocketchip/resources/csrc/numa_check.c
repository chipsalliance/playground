#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <numa.h>

int main() {
	int maxNode = 0, maxCount = 0;
	if (numa_available() < 0) {
		// numa not supported
		maxNode = -1;
		maxCount = 1;
	} else {
		int cpuCount[numa_max_node() + 1];
		memset(cpuCount, 0, sizeof(int) * (numa_max_node() + 1));
		for (int cpuIndex = 0; cpuIndex < numa_num_configured_cpus(); cpuIndex++) {
			cpuCount[numa_node_of_cpu(cpuIndex)]++;
		}
		for (int nodeIndex = 0; nodeIndex < numa_max_node() + 1; nodeIndex++) {
			if (cpuCount[nodeIndex] > maxCount) {
				maxNode= nodeIndex;
				maxCount = cpuCount[nodeIndex];
			}
		}
	}
	printf("%d\n%d\n", maxNode, maxCount);
}
