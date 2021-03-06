package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;

/** Stage class containing methods for staging files.
 *  @author Chris Seo
 */
public class Stage implements Serializable {

    /** Staged for addition directory. */
    static final File STAGE_DIR = Utils.join(Main.GITLET_DIR,
            "stage");

    /** Staged for removal directory. */
    static final File STAGE_RM_DIR = Utils.join(Main.GITLET_DIR,
            "stage_rm");

    /** Stores staged additions files. */
    static final File STAGED_SAVE = Utils.join(Main.GITLET_DIR,
            "staged_save");

    /** Stores commit tree. */
    static final File TREE_DIR = Tree.TREE_DIR;

    /** Working directory, where user initializes gitlet. */
    static final File WORKING_DIR = Main.WORKING_DIR;

    /** Constructor for the stage. Initializes stage directories. */
    public Stage() {
        STAGE_DIR.mkdir();
        STAGE_RM_DIR.mkdir();

        try {
            STAGED_SAVE.createNewFile();
        } catch (IOException ignored) {
            return;
        }

        _stagedFiles = new HashSet<>();

        save(this);
    }

    /** Adds a file to stage for addition.
     * @param sourceName name of file to be staged */
    public static void add(String sourceName) {
        File source = Utils.join(WORKING_DIR, sourceName);
        Commit currentCommit = Utils.readObject(TREE_DIR, Tree.class).
                getCurrHead();
        if (source.exists()) {
            File dest = Utils.join(STAGE_DIR, sourceName);

            if (currentCommit.getBlobs().containsKey(sourceName)
                && Utils.filesSet(STAGE_RM_DIR).contains(sourceName)) {
                String fileAddedContents = Utils.readContentsAsString(Utils.
                        join(WORKING_DIR, sourceName));
                String fileRemovedContents = Utils.readContentsAsString(Utils.
                        join(STAGE_RM_DIR, sourceName));
                if (fileAddedContents.equals(fileRemovedContents)) {
                    clearRemoved();
                }
            } else if (currentCommit.getBlobs().containsKey(sourceName)) {
                String sourceContents = Utils.readContentsAsString(source);
                String commitContents = currentCommit.getBlobs().
                        get(sourceName).getContents();
                if (sourceContents.equals(commitContents)) {
                    addOrRemoveFromStagedHash(false, sourceName);
                    dest.delete();
                } else {
                    addOrRemoveFromStagedHash(true, sourceName);
                    Utils.copy(source, dest);
                }
            } else {
                addOrRemoveFromStagedHash(true, sourceName);
                Utils.copy(source, dest);
            }
        } else {
            System.out.println("File does not exist.");
            System.exit(0);
        }
    }

    /** Clears all files staged for addition. */
    public static void clear() {
        if (STAGE_DIR.listFiles() != null) {
            for (File file : STAGE_DIR.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
        Stage saved = Utils.readObject(STAGED_SAVE,
                Stage.class);
        HashSet<String> items = saved.getStagedFiles();
        items.clear();

        save(saved);
    }

    /** Clears all files staged for removal. */
    public static void clearRemoved() {
        if (STAGE_RM_DIR.listFiles() != null) {
            for (File file : STAGE_RM_DIR.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
    }

    /** Get all files staged for addition.
     * @return files staged as a HashSet */
    public HashSet<String> getStagedFiles() {
        return _stagedFiles;
    }

    /** Adds or removes from the staged files.
     * @param add add or remove
     * @param thing thing to add/remove */
    public static void addOrRemoveFromStagedHash(Boolean add, String thing) {
        if (add) {
            Stage saved = Utils.readObject(STAGED_SAVE,
                    Stage.class);
            HashSet<String> items = saved.getStagedFiles();
            items.add(thing);
            save(saved);
        } else {
            Stage saved = Utils.readObject(STAGED_SAVE,
                    Stage.class);
            HashSet<String> items = saved.getStagedFiles();
            items.remove(thing);
            save(saved);
        }
    }

    /** Saves thing to stage_saved for persistence.
     * @param thing to be saved */
    public static void save(Stage thing) {
        Utils.writeObject(STAGED_SAVE, thing);
    }

    /** Stores the name of the files in the staging area. */
    private HashSet<String> _stagedFiles;
}
