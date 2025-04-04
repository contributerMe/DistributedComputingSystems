#include <iostream>
#include <pthread.h>
#include <unistd.h>

using namespace std;

pthread_mutex_t mutex; 
pthread_mutex_t write_lock; 

int reader_count = 0; 

void* reader(void* arg) {
    int id = *((int*)arg);

    // Entry section
    pthread_mutex_lock(&mutex);
    reader_count++;
    if (reader_count == 1) {
        pthread_mutex_lock(&write_lock); // First reader locks the writer
    }
    pthread_mutex_unlock(&mutex);

    // Critical section (reading)
    cout << "Reader " << id << " is reading." << endl;
    sleep(1); // Simulate reading

    // Exit section
    pthread_mutex_lock(&mutex);
    reader_count--;
    if (reader_count == 0) {
        pthread_mutex_unlock(&write_lock); // Last reader unlocks the writer
    }
    pthread_mutex_unlock(&mutex);

    return nullptr;
}

void* writer(void* arg) {
    int id = *((int*)arg);

    // Entry section
    pthread_mutex_lock(&write_lock);

    // Critical section (writing)
    cout << "Writer " << id << " is writing." << endl;
    sleep(1); // Simulate writing

    // Exit section
    pthread_mutex_unlock(&write_lock);

    return nullptr;
}

int main() {
    pthread_t readers[2], writers[2];
    int reader_ids[2] = {1, 2};
    int writer_ids[2] = {1, 2};

    // Initialize mutexes
    pthread_mutex_init(&mutex, nullptr);
    pthread_mutex_init(&write_lock, nullptr);

    // Create reader threads
    for (int i = 0; i < 2; i++) {
        pthread_create(&readers[i], nullptr, reader, &reader_ids[i]);
    }

    // Create writer threads
    for (int i = 0; i < 2; i++) {
        pthread_create(&writers[i], nullptr, writer, &writer_ids[i]);
    }

    // Join threads
    for (int i = 0; i < 2; i++) {
        pthread_join(readers[i], nullptr);
    }
    for (int i = 0; i < 2; i++) {
        pthread_join(writers[i], nullptr);
    }

    // Destroy mutexes
    pthread_mutex_destroy(&mutex);
    pthread_mutex_destroy(&write_lock);

    return 0;
}
