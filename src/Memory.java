public class Memory {

    private static final int WORD_SIZE = 4;
    private static final int WORD_COUNT = 4096;
    private static final int BLOCK_SIZE = 16;
    private static final int VM_BLOCK_COUNT = 224;

    private static final int VM = 0;
    private static final int SUPERVISOR = WORD_COUNT - (16*BLOCK_SIZE); //POINTS TO THE BEGINING OF SUPERVISOR MEMORY
    private static final int PAGING = SUPERVISOR - (16*BLOCK_SIZE); //POINTS TO THE BEGINING OF PAGING MEMORY
    private static final int SHARED = SUPERVISOR - (2*BLOCK_SIZE); //POINTS TO THE BEGINING OF SHARED MEMORY

    private static char[][] memory = new char[WORD_COUNT][WORD_SIZE];
    private static boolean[] memoryLocks = new boolean[VM_BLOCK_COUNT];
    private static boolean[] pagingLocks = new boolean[BLOCK_SIZE];
    private static boolean[] sharedMemLocks = new boolean[2];
    private static boolean[] ifWereSwapped = new boolean[BLOCK_SIZE];

    public static void setIfSwapped(int index){
        ifWereSwapped[index] = true;
    }

    public static void setIfSwappedBack(int index){
        ifWereSwapped[index] = false;
    }

    public static int checkSwapIndex(){
        int index = 0;
        while(ifWereSwapped[index]){
            index++;
        }
        return index;
    }

    public static int checkSwapIndexPage(){
        int index = 0;
        while(ifWereSwapped[index]){
            index++;
        }
        return PAGING + 16*index;
    }

    public static void fakeMemoryLocks(){
        for(int i = 0; i < memoryLocks.length; i++){
            if(i%2 == 0){
                memoryLocks[i] = true;
            }
        }
    }

    public static void setPagingLocks(){
        for(int i = 0; i < pagingLocks.length; i++){
            pagingLocks[i] = false;
            ifWereSwapped[i] = false;
        }
    }

    public static void lockPage(int page){
        int address = page - PAGING;

        if(address == 0){
            pagingLocks[address] = true;

        }
        else if(address > 0){
            pagingLocks[address/16] = true;
        }

    }

    public static void unlockPage(int page){
        pagingLocks[page] = false;
    }

    public static int getPager(){
        int index = 0;

        while(pagingLocks[index]){
            index++;
        }
        if(index > 13){
            return -1;
        }
        return PAGING + 16*index;
    }

    public static void unlockLockedMemory(int block){
        memoryLocks[block/BLOCK_SIZE] = false;

    }

    public static int getEmptyBlockAddress() {
        for(int i = 0; i < memoryLocks.length; i++) {
            if(!memoryLocks[i]) {
                memoryLocks[i] = true;
                return i*BLOCK_SIZE;
            }
        }
        return -1;
    }

    public static void lockSharedBlock(int blockIndex){
        sharedMemLocks[blockIndex] = true;
    }

    public static void unlockSharedBlock(int blockIndex){
        sharedMemLocks[blockIndex] = false;
    }

    public static boolean checkShrMemoryLock(int address){
        return !sharedMemLocks[address / BLOCK_SIZE];
    }

    public static void addToMemory(int address, char[] word) {
        memory[address] = word;
    }

    public static void addToSupervisor(int address, char[] word) {
        memory[SUPERVISOR + address] = word;
    }

    public static int getSharedMemoryStart(){
        return SHARED;
    }

    public static int getPagingMemoryStart(){
        return PAGING;
    }

    public static char[][] getMemory() {
        return memory;
    }

    public static char [] getMemory(int address) {
        return memory[address];
    }

    public static int getMemoryInt(int address){
        return Integer.parseInt(String.valueOf(memory[address]), 16);
    }

    public static char[] getFromSupervisor(int address) {
        return memory[SUPERVISOR + address];
    }

    public static void setMemoryLocks(boolean[] memoryLocks) {
        Memory.memoryLocks = memoryLocks;
    }

    public static char[] getStartOfBlock(int blockIndex) {
        if(blockIndex < 0 || blockIndex > (WORD_COUNT / BLOCK_SIZE) - 1) {
            throw new IllegalArgumentException("Incorrect block specified!");
        } else {
            return memory[blockIndex * BLOCK_SIZE];
        }
    }
}
