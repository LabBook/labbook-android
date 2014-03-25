package uk.ac.ic.bss.labbook.calculator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class HistoryEntry {
    private static final int VERSION_1 = 1;
    private String mBase;
    private String mEdited;

    HistoryEntry(String str) {
        mBase = str;
        clearEdited();
    }

    HistoryEntry(int version, DataInput in) throws IOException {
        if (version >= VERSION_1) {
            mBase   = in.readUTF();
            mEdited = in.readUTF();
            //Calculator.log("load " + mEdited);
        } else {
            throw new IOException("invalid version " + version);
        }
    }
    
    void write(DataOutput out) throws IOException {
        out.writeUTF(mBase);
        out.writeUTF(mEdited);
        //Calculator.log("save " + mEdited);
    }

    @Override
    public String toString() {
        return mBase;
    }

    void clearEdited() {
        mEdited = mBase;
    }

    String getEdited() {
        return mEdited;
    }

    void setEdited(String edited) {
        mEdited = edited;
    }

    String getBase() {
        return mBase;
    }
}

