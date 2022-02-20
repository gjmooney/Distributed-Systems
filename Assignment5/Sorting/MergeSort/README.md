# Task 1: Getting Started

### Questions

1. The code consists of the main MergeSort class which creates an array, and sends it to a Branch, the Branch splits the array in half and sends each half to it's children. The Branch's child can be another Branch or a Sorter. Sorters contain a priority queue used to sort the array, Branches pull values from the queue on the Sorters. The structure is like a tree. 

2. 
 - Advantages 
    - Could potentially be faster 
    - Can use multiple machines
    - Solution is scalable 
 - Disadvantages
    - The whole thing breaks if the MergeSort (leader) instance goes down
    - There's more overhead/latency
    
3. By printing the time the algorithm starts and the time it finishes we can see how long the algorithm takes to complete. The default array takes around 1 tenth of a second to run, an array with 100 elements takes about 2 tenths of a second, an array of 1000 elements takes about 1 second, an array of 10,000 elements takes about 5 seconds. 

4. Using 4 sorters takes about 3 seconds to sort 1000 elements, using 4 sorters and 10,000 elements takes about 12 seconds. Clearly adding sorters makes the sorting take much longer due to network communication latency. 

5. This setup generates an absurd amount of traffic. Using 4 sorters and 10,000 elements, Wireshark captured over a million packets. The most obvious way to reduce the traffic would be removing the distributed nature of the algorithm. 


# Task 2: Running it outside of local host
1. I expect the run times to become even longer as the nodes are now further apart and therefore have even more latency. 

2. Sorting just 100 nodes with two sorters on the AWS instance took over a minute and a half due to the increased network latency.

# Task 3: How to improve 
1. Most of the time is lost to the communication between the Branch's and the Sorters. The Branch calls peed on both of it's children to find the lower value, and then makes another remove call to whichever child has the smaller number. So every time a number is sorted that takes at least three messages being sent, which is a lot of overhead. This could be made more efficient by having the sorters sort their arrays locally and sending the sorted array back to the branch instead of doing one number at a time. 

2. To run in parallel the Sorters must be on different systems. If the Sorters are on the same system they will be running concurrently not in parallel. It is already distributed. 

3. It does not makes sense to run this as a distributed algorithm. The time complexity of a merge sort does not change by distribuing it, and the added latency of network communications only makes it take longer to complete. 
