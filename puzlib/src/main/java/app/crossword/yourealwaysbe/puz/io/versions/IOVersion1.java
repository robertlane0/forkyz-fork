package app.crossword.yourealwaysbe.puz.io.versions;

import app.crossword.yourealwaysbe.puz.io.IO;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class IOVersion1 implements IOVersion {

    private Charset CHARSET = Charset.forName("Cp1252");

    public void read(Puzzle puz, DataInputStream dis) throws IOException {
        PuzzleMeta meta = readMeta(dis);
        applyMeta(puz, meta);
        Box[][] boxes = puz.getBoxes();
        for(Box[] row : boxes ){
            for(Box b : row){
                if(Box.isBlock(b)) {
                    continue;
                }
                b.setCheated(dis.readBoolean());
                b.setResponder(IO.readNullTerminatedString(dis, getCharset()));
            }
        }
        try{
            puz.setTime(dis.readLong());
        }catch(IOException ioe){
            ioe.printStackTrace();
        }


    }

    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        puz.setSource(meta.getSource());
        puz.setDate(meta.getDate());
    }

    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = new PuzzleMeta();

        meta.setAuthor(IO.readNullTerminatedString(dis, getCharset()));
        meta.setSource(IO.readNullTerminatedString(dis, getCharset()));
        meta.setTitle(IO.readNullTerminatedString(dis, getCharset()));

        long dateEpoch = dis.readLong();
        meta.setDate(
            (dateEpoch == 0)
                ? null
                : Instant.ofEpochMilli(dateEpoch)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
        );

        meta.setPercentComplete(dis.readInt());
        meta.setPercentFilled(meta.getPercentComplete());

        return meta;
    }

    public void write(Puzzle puz, DataOutputStream dos) throws IOException {
        writeMeta(puz, dos);
        Box[][] boxes = puz.getBoxes();
        for(Box[] row : boxes ){
            for(Box b : row){
                if(Box.isBlock(b)) {
                    continue;
                }
                dos.writeBoolean(b.isCheated());
                IO.writeNullTerminatedString(
                    dos, b.getResponder(), getCharset()
                );
            }
        }
        dos.writeLong(puz.getTime());
    }

    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        IO.writeNullTerminatedString(dos, puz.getAuthor(), getCharset());
        IO.writeNullTerminatedString(dos, puz.getSource(), getCharset());
        IO.writeNullTerminatedString(dos, puz.getTitle(), getCharset());
        LocalDate date = puz.getDate();
        if (date == null) {
            dos.writeLong(0);
        } else {
            dos.writeLong(
                date.atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            );
        }
        dos.writeInt(puz.getPercentComplete());
    }

    @Override
    public Charset getCharset() {
        return CHARSET;
    }
}
