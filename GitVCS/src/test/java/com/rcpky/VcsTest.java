package com.rcpky;

import com.rcpky.command.CommandRegistry;
import com.rcpky.core.Repository;
import com.rcpky.core.WorkingDirectory;
import com.rcpky.exception.VcsException;
import com.rcpky.model.Blob;
import com.rcpky.model.Commit;
import com.rcpky.util.VcsUtils;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;


public class VcsTest {

    private Repository repo;
    private CommandRegistry registry;
    private WorkingDirectory wd;

    @Before
    public void setUp() {
        repo = new Repository("Test User <test@example.com>");
        registry = new CommandRegistry();
        wd = repo.getWorkingDirectory();
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Test
    public void testInit_setsDefaultBranch() {
        registry.execute(repo, new String[]{"init"});
        assertTrue(repo.isInitialized());
        assertEquals("main", repo.getCurrentBranch());
    }

    @Test(expected = VcsException.class)
    public void testInit_doubleInit_throws() {
        registry.execute(repo, new String[]{"init"});
        registry.execute(repo, new String[]{"init"});
    }

    @Test(expected = VcsException.class)
    public void testCommand_beforeInit_throws() {
        registry.execute(repo, new String[]{"status"});
    }

    // ── Add ───────────────────────────────────────────────────────────────────

    @Test
    public void testAdd_matchesRegex_stagesFiles() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("main.java", "class Main {}");
        wd.addFile("readme.md", "hello");
        registry.execute(repo, new String[]{"add", ".*\\.java"});
        assertTrue(repo.getStagingArea().isStaged("main.java"));
        assertFalse(repo.getStagingArea().isStaged("readme.md"));
    }

    @Test
    public void testAdd_noMatch_stagesNothing() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("main.java", "class Main {}");
        registry.execute(repo, new String[]{"add", ".*\\.txt"});
        assertFalse(repo.getStagingArea().isStaged("main.java"));
    }

    // ── Commit ────────────────────────────────────────────────────────────────

    @Test
    public void testCommit_createsCommit_clearsStagingArea() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("file.txt", "content");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "First commit"});

        Commit head = repo.getHeadCommit();
        assertNotNull(head);
        assertEquals("First commit", head.getMessage());
        assertNull(head.getParentHash());
        assertTrue(repo.getStagingArea().isEmpty());
    }

    @Test
    public void testCommit_parentChain() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("a.txt", "a");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "Commit A"});

        wd.addFile("b.txt", "b");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "Commit B"});

        Commit head = repo.getHeadCommit();
        assertEquals("Commit B", head.getMessage());
        Commit parent = repo.getObjectStore().getCommit(head.getParentHash());
        assertEquals("Commit A", parent.getMessage());
    }

    @Test(expected = VcsException.class)
    public void testCommit_nothingStaged_throws() {
        registry.execute(repo, new String[]{"init"});
        registry.execute(repo, new String[]{"commit", "-m", "Empty"});
    }

    @Test
    public void testCommit_am_stagesTrackedFiles() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("tracked.txt", "v1");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "Initial"});

        wd.addFile("tracked.txt", "v2");
        registry.execute(repo, new String[]{"commit", "-am", "Update tracked"});

        Commit head = repo.getHeadCommit();
        assertEquals("Update tracked", head.getMessage());
        String blobHash = repo.getObjectStore().getTree(head.getTreeHash()).getEntries().get("tracked.txt");
        Blob blob = repo.getObjectStore().getBlob(blobHash);
        assertEquals("v2", blob.getContent());
    }

    // ── Checkout ──────────────────────────────────────────────────────────────

    @Test
    public void testCheckout_createBranch_switchesBranch() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("f.txt", "data");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "C1"});

        registry.execute(repo, new String[]{"checkout", "-b", "dev"});
        assertEquals("dev", repo.getCurrentBranch());
    }

    @Test
    public void testCheckout_branchIsolation() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("common.txt", "shared");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "Shared commit"});

        registry.execute(repo, new String[]{"checkout", "-b", "feature"});
        wd.addFile("feature.txt", "feature");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "Feature commit"});

        registry.execute(repo, new String[]{"checkout", "main"});
        assertFalse("feature.txt must not exist on main", wd.hasFile("feature.txt"));
        assertTrue("common.txt must exist on main", wd.hasFile("common.txt"));
    }

    @Test(expected = VcsException.class)
    public void testCheckout_nonExistentBranch_throws() {
        registry.execute(repo, new String[]{"init"});
        registry.execute(repo, new String[]{"checkout", "ghost"});
    }

    @Test(expected = VcsException.class)
    public void testCheckout_duplicateBranch_throws() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("x.txt", "x");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "C"});
        registry.execute(repo, new String[]{"checkout", "-b", "feature"});
        registry.execute(repo, new String[]{"checkout", "-b", "feature"});
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test
    public void testReset_oneStep_restoressCommit() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("v.txt", "version1");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "V1"});

        wd.addFile("v.txt", "version2");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "V2"});

        registry.execute(repo, new String[]{"reset", "HEAD~1"});
        assertEquals("V1", repo.getHeadCommit().getMessage());
        assertEquals("version1", wd.getContent("v.txt"));
    }

    @Test(expected = VcsException.class)
    public void testReset_beyondRoot_throws() {
        registry.execute(repo, new String[]{"init"});
        wd.addFile("a.txt", "a");
        registry.execute(repo, new String[]{"add", ".*"});
        registry.execute(repo, new String[]{"commit", "-m", "Only commit"});
        registry.execute(repo, new String[]{"reset", "HEAD~2"});
    }

    // ── Blob & SHA-256 ────────────────────────────────────────────────────────

    @Test
    public void testBlob_sameContent_sameHash() {
        Blob b1 = new Blob("hello");
        Blob b2 = new Blob("hello");
        assertEquals(b1.getHash(), b2.getHash());
    }

    @Test
    public void testBlob_differentContent_differentHash() {
        Blob b1 = new Blob("hello");
        Blob b2 = new Blob("world");
        assertNotEquals(b1.getHash(), b2.getHash());
    }

    @Test
    public void testVcsUtils_sha256_length() {
        String hash = VcsUtils.sha256("test");
        assertEquals(64, hash.length());
    }

    // ── Unknown command ───────────────────────────────────────────────────────

    @Test(expected = VcsException.class)
    public void testUnknownCommand_throws() {
        registry.execute(repo, new String[]{"init"});
        registry.execute(repo, new String[]{"rebase"});
    }
}
