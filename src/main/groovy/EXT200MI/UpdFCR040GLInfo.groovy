/**
 * README
 * This extension is used by ARS110/I.
 * Parameter inputs DIVI, STCF, CHID and update EXN1, EXI1
 *
 * Name: EXT200MI.UpdFCR040GLInfo
 * Description: Get record changed by current user and update GL information fields EXN1 and EXI1
 * Date      Changed By  Description
 * 20210413  CGARCHIT CRMT007 - ARS110 – Add info type 239 with order no. field
 */

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UpdFCR040GLInfo extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;
  private final LoggerAPI logger;

  public UpdFCR040GLInfo(MIAPI mi, DatabaseAPI database, ProgramAPI program, LoggerAPI logger) {
    this.mi = mi;
    this.database = database;
    this.program = program;
    this.logger = logger;
  }

  public void main() {
    int inCONO;
    String inDIVI;
    String inSTCF;
    String inCHID;
    String inEXN1;
    String inEXI1;
    if (mi.in.get("CONO") == null) {
      inCONO = program.LDAZD.CONO;
    } else {
      inCONO = mi.in.get("CONO");
    }
    inDIVI = mi.in.get("DIVI") == null ? "" : mi.in.get("DIVI");
    inSTCF = mi.in.get("STCF") == null ? "" : mi.in.get("STCF");
    inCHID = mi.in.get("CHID") == null ? "" : mi.in.get("CHID");
    inEXN1 = mi.in.get("EXN1") == null ? "" : mi.in.get("EXN1");
    inEXI1 = mi.in.get("EXI1") == null ? "" : mi.in.get("EXI1");

    String EXN1 = "";
    String EXI1 = "";

    Date currDate = new Date();
    java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMdd");
    String currDateFormatted = formatter.format(currDate);

    LocalDateTime dateTime = LocalDateTime.now();//
    String time = dateTime.format(DateTimeFormatter.ofPattern("HHmmss")); //
    int currTimeFormatted = time.toInteger();//

    String vUSER = program.getUser();

    // Do selection query on FCR040
    DBAction dbaFCR040 = database.table("FCR040").index("06").selection("ACCONO", "ACDIVI", "ACSTCF", "ACCHID").build();
    DBContainer conFCR040 = dbaFCR040.getContainer();

    conFCR040.set("ACCONO", inCONO);
    conFCR040.set("ACDIVI", inDIVI);
    conFCR040.set("ACSTCF", inSTCF);
    conFCR040.set("ACCHID", inCHID);
    conFCR040.set("ACEXN1", inEXN1);
    conFCR040.set("ACEXI1", inEXI1);

    if (!dbaFCR040.read(conFCR040)) {
      mi.error("The accounting transaction does not exist.");
      return;
    }

    if (dbaFCR040.read(conFCR040)) {
      EXN1 = conFCR040.get("ACEXN1");
      EXI1 = conFCR040.get("ACEXI1");

      Closure<?> updateFCR040CallBack = { LockedResult lockedResult ->
        // GL information number
        if (inEXN1.trim() == "" || inEXN1.trim() == "?") {
          if (inEXN1.trim() == "") {
            lockedResult.set("ACEXN1", EXN1);
          }
          if (inEXN1.trim() == "?") {
            lockedResult.set("ACEXN1", "");
          }
        } else {
          lockedResult.set("ACEXN1", inEXN1);
        }

        // GL additional information
        if (inEXI1.trim() == "" || inEXI1.trim() == "?") {
          if (inEXI1.trim() == "") {
            lockedResult.set("ACEXI1",EXI1);
          }
          if (inEXI1.trim() == "?") {
            lockedResult.set("ACEXI1", "");
          }
        } else {
          lockedResult.set("ACEXI1", inEXI1);
        }

        lockedResult.set("ACLMDT", currDateFormatted.toInteger());
        lockedResult.update();
      }
      dbaFCR040.readLock(conFCR040, updateFCR040CallBack);
    } else {
      mi.error("No record found.");
      return;
    }
  }
}
