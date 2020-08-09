import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Scanner;

public class RealMachine {
    private static int ra;      //bendros paskirties pirmasis registras
    private static int rb;      //bendros paskirties antrasis registras

    private static short ic;    //virtualios masinos programos skaitiklis
    private static byte sf;     //pozymiu registras

    private static int ptr;     //puslapiu lenteles registras

    private static byte pi;     //programiniu pertrukimu registras
    private static byte si;     //supervizoriniu pertraukimu registras
    private static byte ti;     //taimerio registras

    private static int cm;     //registras rodantis i bendra atminti

    private static byte mode;   //registras nusakantis procesoriaus darbo rezima (0 - supervizorinis; 1 - vartotojas)
    private static byte ch1;    //pirmasis kanalu registras jungiantis klaviatura su vartotojo atmintimi
    private static byte ch2;    //antrasis kanalu registras jungiantis ekrana su vartotojo atmintimi
    private static byte ch3;    //treciasis kanalu registras jungiantis kietaji diska su vartotojo amtintimi

//    private static final String HDD = "D:\\University shit\\Operating Systems\\OS\\hdd.txt";

    RealMachine() throws IOException {
        ra = 0;
        rb = 0;
        ic = 0;
        sf = 0;
        ptr = 0;
        ti = 30;

        cm = Memory.getSharedMemoryStart();

        mode = 0;
        ch1 = 0;
        ch2 = 0;
        ch3 = 0;
        Memory.setPagingLocks();
        for(int i = 0; i < 14; i++){
            runVM("D:\\Studijos\\6 Semestras\\OS\\FinalOS\\code.txt");
        }
        startMenu();
    }

    private static void startMenu() throws IOException {
        while(true) {
            System.out.println();
            System.out.println("MENU");
            System.out.println("1. RUN VM");
            System.out.println("2. PRINT ALL REGISTERS");
            System.out.println("3. PRINT MEMORY");
            System.out.println("4. SWAP MEMORY");
            System.out.println("5. RUN VM FROM SIDE MEMORY.");
            System.out.println("10. EXIT");
            Scanner input = new Scanner(System.in);
            int value = input.nextInt();

            switch (value) {
                case 1 :
                    Scanner input2 = new Scanner(System.in);
                    System.out.println("ENTER FILENAME TO RUN VM FROM: ");
                    String filename = input2.nextLine();
                    runVM("D:\\Studijos\\6 Semestras\\OS\\FinalOS\\" + filename);
                break;
                case 2 :
                    printAllRegisters();
                    break;
                case 3 :
                    printAllMemory();
                    break;
                case 4 :
                    break;
                case 5 :
                    runVMFromSwap();
                    break;
                case 10 :
                    File inFile = new File("D:\\Studijos\\6 Semestras\\OS\\FinalOS\\code1.txt");



                    if (!inFile.delete()) {
                        System.out.println("Could not delete file");
                        return;
                    }

                    Path path = Paths.get("D:\\Studijos\\6 Semestras\\OS\\FinalOS\\code1.txt");

                    Files.createDirectories(path.getParent());

                    try {
                        Files.createFile(path);
                    } catch (FileAlreadyExistsException e) {
                        System.err.println("already exists: " + e.getMessage());
                    }


                    System.exit(0);
                    break;
                default :
                    System.out.println("BAD INPUT!!!");
                    break;
            }
        }
    }

    private static void printAllMemory() {
        System.out.print("0::");
        for(int i = 0; i < Memory.getMemory().length - 1; ++i) {
            System.out.print(String.format("%"+4+"s", String.valueOf(Memory.getMemory(i)) + "|"));
            if((i+1)%16==0){
                System.out.println();
                System.out.print(Integer.toHexString(i+1).toUpperCase() + ":: ");
            }
        }
        System.out.println();
    }

    private static void printAllRegisters() {
        System.out.println("---------REGISTERS--------");
        System.out.println("RA: " + Integer.toHexString(getRa()));
        System.out.println("RB: " + Integer.toHexString(getRb()));
        System.out.println("IC: " + Integer.toHexString(getIc()));
        System.out.println("SF: " + Integer.toHexString(getSf()));
        System.out.println("PTR: " + Integer.toHexString(ptr));
        System.out.println("------");
        System.out.println("MODE: " + Integer.toHexString(mode));
        System.out.println("CH1: " + Integer.toHexString(ch1));
        System.out.println("CH2: " + Integer.toHexString(ch2));
        System.out.println("CH3: " + Integer.toHexString(ch3));
        System.out.println("--------------------------");
    }

    private static void runVM(String path){
        try{
            clearSupervisorMemory();
            loadCode(path);
            checkInterrupts();
            memoryAllocation();
            addCodeToVM();
            setMode((byte)1);
            VirtualMachine vm = new VirtualMachine();
            readCode(vm);
            setMode((byte) 0);
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private static void runVMFromSwap(){
        try{
            clearSupervisorMemory();
            loadCodeFromSwap();
            checkInterrupts();
            clearPtrMemory();
            addCodeToVM();
            setMode((byte)1);
            VirtualMachine vm = new VirtualMachine();
            readCode(vm);
            setMode((byte) 0);
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private static void runVMSwap(){
        try{
            checkInterrupts();
            addCodeToVM();
            setMode((byte)1);
            VirtualMachine vm = new VirtualMachine();
            readCode(vm);
            setMode((byte) 0);
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private static void clearPager(int page){
        int index = 0;
        for(int i = 0; i < 16; i++){
            Memory.addToMemory(page + index++, new char[4]);
        }
    }

    private static void clearSupervisorMemory(){
        ra = 0;
        rb = 0;
        ic = 0;
        sf = 0;
        ptr = 0;
        ti = 30;

        cm = Memory.getSharedMemoryStart();

        mode = 0;
        ch1 = 0;
        ch2 = 0;
        ch3 = 0;
        si = 0;
        pi = 0;
        int index = 0;
        while(Memory.getFromSupervisor(index)[0] != 0
                || Memory.getFromSupervisor(index)[1] != 0
                || Memory.getFromSupervisor(index)[2] != 0
                || Memory.getFromSupervisor(index)[3] != 0){
            System.out.println("clear");
            char[] filler = new char[4];
            Memory.addToSupervisor(index, filler);
            index++;
        }
    }

    private static void loadCode(String codeName) throws IOException {
        setMode((byte) 0);
        setCh3((byte) 1);

        String line;
        String[] tokens;
        try(BufferedReader br = new BufferedReader(new FileReader(codeName))) {
            int wordIndex = 0;

            while((line = br.readLine()) != null){
                //Checks if code is not too big
                if(wordIndex >= 128) {
                    throw new Exception("4");
                }

                if(checkCode(line)){
                    //DW and DD procedures are unique in a sense, that they take up 2 words
                    if(line.matches("[D][W|D]\\s.+")){
                        tokens = line.split(" ");
                        Memory.addToSupervisor(wordIndex, tokens[0].toCharArray());
                        wordIndex++;

                        if(wordIndex >= 128) {
                            throw new Exception("4");
                        }
                        if(tokens[1].length() > 4) {
                            Memory.addToSupervisor(wordIndex, tokens[1].substring(0, 4).toCharArray());
                            wordIndex++;
                            if(wordIndex >= 128) {
                                throw new Exception("4");
                            }
                            Memory.addToSupervisor(wordIndex, tokens[1].substring(4).toCharArray());
                            wordIndex++;
                        } else {
                            Memory.addToSupervisor(wordIndex, tokens[1].toCharArray());
                            wordIndex++;
                        }
                    } else {
                        Memory.addToSupervisor(wordIndex, line.toCharArray());
                        wordIndex++;
                    }
                }
            }
            if(!String.valueOf(Memory.getFromSupervisor(wordIndex - 1)).equals("HALT")){
                throw new Exception("1");
            }
        } catch (FileNotFoundException fe) {
            System.out.println("FILE DOESN'T EXIST!");
            startMenu();
        } catch (Exception e) {
            if(e.getMessage().substring(0, 1).matches("\\d")) {
                setPi(Byte.parseByte(e.getMessage().substring(0, 1)));
            }
        }

        setMode((byte) 1);
        setCh3((byte) 0);
    }

    private static void loadCodeFromSwap() throws IOException {
        setMode((byte) 0);
        setCh3((byte) 1);

        System.out.println("ENTER WHICH PAGE YOU WANT TO SWAP:");
        Scanner input = new Scanner(System.in);
        int value = input.nextInt();

        String line, codeName = "D:\\Studijos\\6 Semestras\\OS\\FinalOS\\code1.txt";
        String[] tokens;
        try(BufferedReader br = new BufferedReader(new FileReader(codeName))) {
            int wordIndex = 0;
            boolean read = false;

            while((line = br.readLine()) != null) {
                //Checks if code is not too big

                if (line.equals(String.valueOf(value))) {
                    read = true;

                    ptr = Memory.getPagingMemoryStart() + 16*value;
                    continue;
                }
                System.out.println(line + " " + read);
                if(read){
                    if(line.equals("---")){
                        break;
                    }
                    if (wordIndex >= 128) {
                        throw new Exception("4");
                    }

                    if (checkCode(line)) {
                        //DW and DD procedures are unique in a sense, that they take up 2 words
                        if (line.matches("[D][W|D]\\s.+")) {
                            tokens = line.split(" ");
                            Memory.addToSupervisor(wordIndex, tokens[0].toCharArray());
                            wordIndex++;

                            if (wordIndex >= 128) {
                                throw new Exception("4");
                            }
                            if (tokens[1].length() > 4) {
                                Memory.addToSupervisor(wordIndex, tokens[1].substring(0, 4).toCharArray());
                                wordIndex++;
                                if (wordIndex >= 128) {
                                    throw new Exception("4");
                                }
                                Memory.addToSupervisor(wordIndex, tokens[1].substring(4).toCharArray());
                                wordIndex++;
                            } else {
                                Memory.addToSupervisor(wordIndex, tokens[1].toCharArray());
                                wordIndex++;
                            }
                        } else {
                            Memory.addToSupervisor(wordIndex, line.toCharArray());
                            wordIndex++;
                        }
                    }
                }
            }
            if(!String.valueOf(Memory.getFromSupervisor(wordIndex - 1)).equals("HALT")){
                throw new Exception("1");
            }
        } catch (FileNotFoundException fe) {
            System.out.println("FILE DOESN'T EXIST!");
            startMenu();
        } catch (Exception e) {
            if(e.getMessage().substring(0, 1).matches("\\d")) {
                setPi(Byte.parseByte(e.getMessage().substring(0, 1)));
            }
        }
        Memory.setIfSwappedBack(value);
        removeLineFromFile(String.valueOf(value));
        setMode((byte) 1);
        setCh3((byte) 0);
    }

    private static boolean checkCode(String token) throws Exception{
        switch (token) {
            case "CODE":
                return true;
            case "ADD" :
                return true;
            case "SUB" :
                return true;
            case "CMP" :
                return true;
            case "HALT" :
                return true;
            case "PRTS" :
                return true;
            case "PRTN" :
                return true;
            case "READ" :
                return true;
            case "DATA" :
                return true;
            case "DN" :
                return true;
            default:
                break;
        }

        if(token.matches("[M][A|B](\\d|[A-F])(\\d|[A-F])")){
            if(Integer.parseInt(token.substring(2), 16) >= 128
                    && Integer.parseInt(token.substring(2), 16) <= 255) {
                return true;
            }
            throw new Exception("3" + token);
        } else if(token.matches("[A|B][M](\\d|[A-F])(\\d|[A-F])")){
            if(Integer.parseInt(token.substring(2), 16) >= 128
                    && Integer.parseInt(token.substring(2), 16) <= 255) {
                return true;
            }
            throw new Exception("3" + token);
        } else if(token.matches("(([S][A])|([A][S]))(\\d|[A-F])(\\d|[A-F])")){
            if(Integer.parseInt(token.substring(2), 16) >= 0
                    && Integer.parseInt(token.substring(2), 16) <= 31) {
                return true;
            }
            throw new Exception("3" + token);
        } else if(token.matches("[U|L][C].")){
            if(token.matches("..(\\d|[A-F])")){
                return true;
            }
            throw new Exception("3" + token);
        } else if(token.matches("[J][P|E|B|A](\\d|[A-F])(\\d|[A-F])")){
            if(Integer.parseInt(token.substring(2), 16) >= 0
                    && Integer.parseInt(token.substring(2), 16) <= 127) {
                return true;
            }
            throw new Exception("3" + token);
        } else if(token.matches("[P](\\d|[A-F]){3}")) {
            if(Integer.parseInt(token.substring(2, 3), 16) <= Integer.parseInt(token.substring(3, 4), 16)
                    && Integer.parseInt(token.substring(2,3), 16) <= 15
                    && Integer.parseInt(token.substring(1,2), 16) >= 8
                    && Integer.parseInt(token.substring(1,2), 16) <= 15) {
                return true;
            }
            throw new Exception("3" + token);
        } else if(token.matches("[D][W]\\s(\\d|[A-F]){1,8}")) {
            return true;
        } else if(token.matches("[D][D]\\s\\w{1,4}")){
            return true;
        }
        throw new Exception("1");
    }

    private static void memoryAllocation() {

        if(Memory.getPager() != -1) {
            ptr = Memory.getPager();
            Memory.lockPage(ptr);
            for (int i = 0; i < 16; i++) {
                Memory.addToMemory(ptr + i, Integer.toHexString(Memory.getEmptyBlockAddress()).toCharArray());
            }
        } else {
            swapMemory();
        }
    }

    private static void addCodeToVM(){
        int count = 0;
        for(int i = ptr; i < ptr + 8; ++i) {
            if(Memory.getFromSupervisor(count)[0] != 0){
                for(int j = 0; j < 16; ++j){
                    Memory.addToMemory(Memory.getMemoryInt(i) + j, Memory.getFromSupervisor(count));
                    count++;
                }
            } else {
                return;
            }
        }
    }

    private static void swapMemory(){

        addCodeToSideMemory();

        clearPtrMemory();

        runVMSwap();
    }

    private static void readCode(VirtualMachine vm) throws Exception {
        while(!String.valueOf(Objects.requireNonNull(getWordFromMemory(ic))).equals("HALT")){
            vm.execute(String.valueOf(Objects.requireNonNull(getWordFromMemory(ic))));
            checkInterrupts();
            increaseIc();
        }
        if(String.valueOf(Objects.requireNonNull(getWordFromMemory(ic))).equals("HALT")){
            setSi((byte) 5);        //Setting HALT interrupt
        }
    }

    private static void addCodeToSideMemory() {
        int index = 0;
        ptr = Memory.checkSwapIndexPage();
        int indexx = Memory.checkSwapIndex();
        Memory.setIfSwapped(indexx);

        BufferedWriter writer;
        String line;
        try {
            writer = new BufferedWriter(new FileWriter("D:\\Studijos\\6 Semestras\\OS\\FinalOS\\code1.txt",true));


            writer.write(indexx + "\n");
        while(index <= 127){

            line = String.valueOf(Objects.requireNonNull(getWordFromMemory(index)));
            if(line.matches("[D][W|D]")){
                if(String.valueOf(Objects.requireNonNull(getWordFromMemory(index + 2))).matches("[D][W|D]") ||
                    String.valueOf(Objects.requireNonNull(getWordFromMemory(index + 2))).equals("CODE")){

                    line = line + " " + String.valueOf(Objects.requireNonNull(getWordFromMemory(index + 1)));
                    index++;
                }
            }

            writer.write(line + "\n");
            if(line.equals("HALT")){
                break;
            }
            index++;
        }
        writer.write("---\n");
        writer.close();

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void removeLineFromFile(String value) {

        try {

            File inFile = new File("D:\\Studijos\\6 Semestras\\OS\\FinalOS\\code1.txt");

            if (!inFile.isFile()) {
                System.out.println("Parameter is not an existing file");
                return;
            }

            //Construct the new file that will later be renamed to the original filename.
            File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

            BufferedReader br = new BufferedReader(new FileReader("D:\\Studijos\\6 Semestras\\OS\\FinalOS\\code1.txt"));
            PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

            String line = null;
            boolean read = false;
            //Read from the original file and write to the new
            //unless content matches data to be removed.
            while ((line = br.readLine()) != null) {
                if(line.equals(value)){
                    read = true;
                }
                if(!read){
                    pw.println(line);
                    pw.flush();
                } else {

                    if (line.trim().equals("---")) {
                        read = false;
                    }
                }


            }
            pw.close();
            br.close();

            //Delete the original file
            if (!inFile.delete()) {
                System.out.println("Could not delete file");
                return;
            }

            //Rename the new file to the filename the original file had.
            if (!tempFile.renameTo(inFile))
                System.out.println("Could not rename file");

        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void clearPtrMemory(){
        for(int i = ptr; i < ptr + 16; ++i) {
            for(int j = 0; j < 16; ++j){
                Memory.addToMemory(Memory.getMemoryInt(i) + j, new char[4]);
            }
        }
    }

    private static void checkInterrupts() throws Exception{
        ti--;
        switch (RealMachine.pi) {
            case 1:
                setMode((byte) 0);
                throw new Exception("INCORRECT OPERATION CODE! VM TERMINATING");
            case 2:
                setMode((byte) 0);
                throw new Exception("INCORRECT ASSIGNMENT! VM TERMINATING");
            case 3:
                setMode((byte) 0);
                throw new Exception("INCORRECT ADDRESS! VM TERMINATING");
            case 4:
                setMode((byte) 0);
                throw new Exception("OVERFLOW! VM TERMINATING");
            default:
                break;
        }
        switch (RealMachine.si) {
            case 1:
                handlePRTS();
                ti -= 2;
                break;
            case 2:
                handlePRTN();
                ti -= 2;
                break;
            case 3:
                handleP();
                ti -= 2;
                break;
            case 4:
                handleREAD();
                ti -= 2;
                break;
            case 5:
                System.out.print("HALT INTERRUPT");
                throw new Exception("HALT INTERRUPT");
            case 6:
                handleLC();
                break;
            case 7:
                handleUC();
                break;
            default:
                break;
        }
        if(ti <= 0){
            setMode((byte) 0);
            System.out.println("TIMER INTERRRUPT");
            ti=30;
            setMode((byte) 1);
        }
    }

    private static void handlePRTS(){
        setMode((byte) 0);
        setCh2((byte) 1);
        System.out.println("----------------EKRANAS----------------");
        System.out.println("|                                     |");
        System.out.println("RA:" + String.valueOf(encodeToAscii(ra)));
        System.out.println("|                                     |");
        System.out.println("---------------------------------------");
        setCh2((byte) 0);
        si = 0;
        setMode((byte) 1);
    }

    private static void handlePRTN(){
        setMode((byte) 0);
        setCh2((byte) 1);
        System.out.println("----------------EKRANAS----------------");
        System.out.println("|                                     |");
        System.out.println("RA:" + Integer.toHexString(ra).toUpperCase());
        System.out.println("|                                     |");
        System.out.println("---------------------------------------");
        setCh2((byte) 0);
        si = 0;
        setMode((byte) 1);
    }

    private static void handleP() {
        setMode((byte) 0);
        int block = Integer.parseInt(String.valueOf(Objects.requireNonNull(getWordFromMemory(ic))).substring(1, 2), 16);
        int y = Integer.parseInt(String.valueOf(Objects.requireNonNull(getWordFromMemory(ic))).substring(2, 3), 16);
        int z = Integer.parseInt(String.valueOf(Objects.requireNonNull(getWordFromMemory(ic))).substring(3, 4), 16);
        setCh2((byte) 1);

        System.out.println("----------------EKRANAS----------------");
        System.out.println("|                                     |");
        for(int i = y; i <= z; i++) {
            System.out.print(Objects.requireNonNull(getWordFromMemory(block * 16 + i)));
        }
        System.out.println();
        System.out.println("|                                     |");
        System.out.println("---------------------------------------");
        setCh2((byte) 0);
        si = 0;
        setMode((byte) 1);
    }

    private static void handleREAD(){
        setMode((byte) 0);
        setCh1((byte) 1);
        System.out.println("this is read: ");
        Scanner input = new Scanner(System.in);
        String value = input.nextLine();
        if(value.length() > 8){
            setPi((byte) 4);
            return;
        } else if(!value.matches("\\p{XDigit}{1,8}")) {
        setPi((byte) 2);
            return;
        }
        setRa(Integer.parseInt(value, 16));
        //setSi((byte) 0);
        setCh1((byte) 0);
        si = 0;
        setMode((byte) 1);
    }

    private static void handleLC(){
        setMode((byte) 0);
        int index = Integer.parseInt(String.valueOf(Objects.requireNonNull(getWordFromMemory(ic))).substring(2), 16);
        if(index >= 0 && index <= 1){
            Memory.lockSharedBlock(index);
        } else RealMachine.setPi((byte) 3);
        si = 0;
        setMode((byte) 1);
    }

    private static void handleUC(){
        setMode((byte) 0);
        int index = Integer.parseInt(String.valueOf(Objects.requireNonNull(getWordFromMemory(ic))).substring(2), 16);
        if(index >= 0 && index <= 1){
            Memory.unlockSharedBlock(index);
        } else RealMachine.setPi((byte) 3);
        si = 0;
        setMode((byte) 1);
    }

    public static void addWordToMemory(char [] word){
        for(int i = ptr + 8; i < ptr + 16; ++i) {
            for(int j = 0; j < 16; ++j){
                if(Memory.getMemory(Memory.getMemoryInt(i) + j)[0] == 0
                    && Memory.getMemory(Memory.getMemoryInt(i) + j)[1] == 0
                    && Memory.getMemory(Memory.getMemoryInt(i) + j)[2] == 0
                    && Memory.getMemory(Memory.getMemoryInt(i) + j)[3] == 0) {
                    Memory.addToMemory(Memory.getMemoryInt(i) + j, word);
                    return;
                }
            }
        }
    }

    public static void addWordToMemory(char [] word, int index){
        if(index <= 255 && index >= 128) {
            Memory.addToMemory(Memory.getMemoryInt(ptr + index/16) + index%16, word);
        } else {
            setPi((byte) 2);
        }
    }

    public static void addWordToSharedMemory(char [] word, int index){
        if(index >= 0 && index < 32) {
            if(Memory.checkShrMemoryLock(index)){
                Memory.addToMemory(cm + index, word);
            } else setPi((byte) 2);
        } else setPi((byte) 2);
    }

    public static char[] getWordFromMemory(int index){
        if(index <= 255) {
            return Memory.getMemory(Memory.getMemoryInt(ptr + index/16) + index%16);
        } else {
            setPi((byte) 2);
            return null;
        }
    }

    public static char[] getWordFromSharedMemory(int index){
        if(index >= 0 && index < 32) {
            return Memory.getMemory(cm + index);
        } else {
            setPi((byte) 2);
            return null;
        }
    }

    public static char[] encodeToAscii(int number){
        char [] codedNumber = new char[4];
        String hexNumber = Integer.toHexString(number);
        if(hexNumber.length()%2 != 0){
            hexNumber = "0" + hexNumber;
        }
        String [] bytes = hexNumber.split("(?<=\\G.{2})");

        int byteIndex = 0;
        for(int i = 4 - bytes.length; i < 4; i++){
           // System.out.println(bytes[byteIndex]);
            codedNumber[i] = (char) Integer.parseInt(bytes[byteIndex], 16);
            byteIndex++;
//            if(i == 3)
//                break;
        }
//        for(int i = 3; i >= 0; i--){
//
//            System.out.println(bytes[i]);
//            codedNumber[i] = (char) Integer.parseInt(bytes[i], 16);
//            if(i == 3)
//                break;
//        }
        return codedNumber;
    }

    public static char[] encodeToAscii(String hexNumber){
        char [] codedNumber = new char[4];
        if(hexNumber.length()%2 != 0){
            hexNumber = "0" + hexNumber;
        }
        String [] bytes = hexNumber.split("(?<=\\G.{2})");

        int byteIndex = 0;
        for(int i = 4 - bytes.length; i < 4; i++){
            codedNumber[i] = (char) Integer.parseInt(bytes[byteIndex], 16);
            byteIndex++;
        }
        System.out.println(codedNumber);
        return codedNumber;
    }

    public static int decodeFromAscii(char [] word){
        String decoded = new String();
            if (Long.parseLong(Integer.toString((int) word[0], 16), 16) >= 128) {
                for (char aWord : word) {
                    int temp = ~(int) Long.parseLong(Integer.toString((int) aWord, 16), 16);
                    //temp = ~temp;
                    String s = Integer.toHexString(temp);
                    decoded = decoded + s.substring(s.length() - 2);
                }
                return (-1) * (Integer.parseInt(decoded, 16) + 1);
            }
            else {
                for (char aWord : word) {
                    decoded = decoded + String.format("%02x", (int) aWord);
                }
            }

        if(decoded.isEmpty()){
            return 0;
        }
        return Integer.parseInt(decoded, 16);
    }

    public static int getRa() {
        return ra;
    }

    public static int getRb() {
        return rb;
    }

    public static short getIc() {
        return ic;
    }

    private static byte getSf() {
        return sf;
    }

    public static void setRa(int ra) {
        RealMachine.ra = ra;
    }

    public static void setRb(int rb) {
        RealMachine.rb = rb;
    }

    public static void setIc(short ic) {
        RealMachine.ic = ic;
    }

    public static void increaseIc() {
        RealMachine.ic++;
    }

    public static void setSf(byte sf) {
        RealMachine.sf = sf;
    }

    public static void setZF(boolean status){
        if(status) {
            sf = (byte) (sf | (1 << 2));
        } else {
            sf = (byte) (sf & ~(1 << 2));
        }
    }

    public static boolean getZF(){
        return (((sf >> 2) & 1) == 1);
    }

    public static void setCF(boolean status){
        if(status) {
            sf = (byte) (sf | (1 << 1));
        } else {
            sf = (byte) (sf & ~(1 << 1));
        }
    }

    public static boolean getCF(){
        return (((sf >> 1) & 1) == 1);
    }

    private static void setMode(byte mode) {
        RealMachine.mode = mode;
        if(mode == 0){
            System.out.println("MODE CHANGED TO SUPERVISOR");
        } else {
            System.out.println("MODE CHANGED TO USER");
        }
    }

    private static void setCh1(byte ch1) {
        RealMachine.ch1 = ch1;
    }

    private static void setCh2(byte ch2) {
        RealMachine.ch2 = ch2;
    }

    private static void setCh3(byte channel) {
        RealMachine.ch3 = channel;
    }

    private static void setPi(byte pi) {
        setMode((byte) 0);
        RealMachine.pi = pi;
        setMode((byte) 1);
    }

    public static void setSi(byte si) {
        setMode((byte) 0);
        RealMachine.si = si;
        setMode((byte) 1);
    }

}
