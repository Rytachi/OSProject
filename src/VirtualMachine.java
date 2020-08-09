
public class VirtualMachine {
    VirtualMachine() {

    }

    public void execute(String process){
        System.out.println(process);
        switch(process) {
            case "DATA" :
                System.out.println("DATA MOVING PROCESS START");
                break;
            case "DW" :
                processDW();
                break;
            case "DD" :
                processDD();
                break;
            case "CODE" :
                System.out.println("CODE START");
                break;
            case "ADD":
                processADD();
                break;
            case "SUB":
                processSUB();
                break;
            case "CMP":
                processCMP();
                break;
            case "PRTS":
                processPRTS();
                break;
            case "PRTN":
                processPRTN();
                break;
            case "READ":
                processREAD();
                break;
            default:
                break;
        }

        if(process.matches("[M][A](\\d|[A-F]){2}")){
            processMA(Integer.parseInt(process.substring(2), 16));
        } else if(process.matches("[M][B](\\d|[A-F]){2}")){
            processMB(Integer.parseInt(process.substring(2), 16));
        } else if(process.matches("[A][M](\\d|[A-F]){2}")){
            processAM(Integer.parseInt(process.substring(2), 16));
        } else if(process.matches("[B][M](\\d|[A-F]){2}")){
            processBM(Integer.parseInt(process.substring(2), 16));
        } else if(process.matches("[S][A](\\d|[A-F])(\\d|[A-F])")){
            processSA(Integer.parseInt(process.substring(2), 16));
        } else if(process.matches("[A][S](\\d|[A-F])(\\d|[A-F])")){
            processAS(Integer.parseInt(process.substring(2), 16));
        } else if(process.matches("[U][C].")){
            processUC();
        } else if(process.matches("[L][C].")){
            processLC();
        } else if(process.matches("[P](\\d|[A-F]){3}")) {
            processP();
        } else if(process.matches("[J][P|E|B|A](\\d|[A-F])(\\d|[A-F])")){
            processJMP(process.substring(0,2), Integer.parseInt(process.substring(2), 16));
        }
    }

    private void processDW(){
        RealMachine.increaseIc();

        String strNum = String.valueOf(RealMachine.getWordFromMemory(RealMachine.getIc()));

        if(String.valueOf(RealMachine.getWordFromMemory(RealMachine.getIc() + 1)).matches("(\\d|[A-F]){1,4}")
            && String.valueOf(RealMachine.getWordFromMemory(RealMachine.getIc() + 2)).matches("([D][D|W])|([C][O][D][E])")){
            RealMachine.increaseIc();
            strNum =  strNum + String.valueOf(RealMachine.getWordFromMemory(RealMachine.getIc()));
        }
        //int number = Integer.parseInt(String.valueOf(RealMachine.getWordFromMemory(RealMachine.getIc())), 16);
        RealMachine.addWordToMemory(RealMachine.encodeToAscii(strNum));
    }

    private void processDD(){
        RealMachine.increaseIc();
        RealMachine.addWordToMemory(RealMachine.getWordFromMemory(RealMachine.getIc()));
    }

    private void processMA(int address){
        System.out.println(RealMachine.decodeFromAscii(RealMachine.getWordFromMemory(address)));

        RealMachine.setRa(RealMachine.decodeFromAscii(RealMachine.getWordFromMemory(address)));
        System.out.println("RA: " + RealMachine.getRa());
    }

    private void processMB(int address){
        RealMachine.setRb(RealMachine.decodeFromAscii(RealMachine.getWordFromMemory(address)));
        System.out.println("RB: " + RealMachine.getRb());
    }

    private void processAM(int address){
        RealMachine.addWordToMemory(RealMachine.encodeToAscii(RealMachine.getRa()), address);
    }

    private void processBM(int address){
        RealMachine.addWordToMemory(RealMachine.encodeToAscii(RealMachine.getRb()), address);
    }

    private void processPRTS(){
        RealMachine.setSi((byte) 1);
    }

    private void processPRTN(){
        RealMachine.setSi((byte) 2);
    }

    private void processP(){
        RealMachine.setSi((byte) 3);
    }

    private void processREAD(){
        RealMachine.setSi((byte) 4);
    }

    private void processADD(){
        RealMachine.setRa(RealMachine.getRb() + RealMachine.getRa());
        System.out.println("RA after ADD: " + RealMachine.getRa());
    }

    private void processSUB(){
        RealMachine.setRa(RealMachine.getRa() - RealMachine.getRb());
        System.out.println("RA after SUB: " + RealMachine.getRa());
    }

    private void processCMP(){
        if(RealMachine.getRa() == RealMachine.getRb()) {
            RealMachine.setZF(true);
            System.out.println("CMP: RA = RB");
        } else if(RealMachine.getRa() < RealMachine.getRb()) {
            RealMachine.setCF(true);
            System.out.println("CMP: RA < RB");
        } else if(RealMachine.getRa() > RealMachine.getRb()) {
            RealMachine.setCF(false);
            System.out.println("CMP: RA > RB");
        }
    }

    private void processSA(int index){
        RealMachine.setRa(RealMachine.decodeFromAscii(RealMachine.getWordFromSharedMemory(index)));
    }

    private void processAS(int index){
        RealMachine.addWordToSharedMemory(RealMachine.encodeToAscii(RealMachine.getRa()), index);
    }

    private void processLC(){
        RealMachine.setSi((byte) 6);
    }

    private void processUC(){
        RealMachine.setSi((byte) 7);
    }

    private void processJMP(String type, int address) {
        if ((type.equals("JP"))
            || (type.equals("JE") && RealMachine.getZF())
            || (type.equals("JB") && RealMachine.getCF())
            || (type.equals("JA") && !RealMachine.getZF() && !RealMachine.getCF())) {
            RealMachine.setIc((short) (address - 1));
        }
    }

}
