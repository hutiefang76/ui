package com.rethinkj.ui.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rethinkj.ui.App;
import com.rethinkj.ui.Script;
import com.rethinkj.ui.examples.Showcase.Look;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShowcaseTest {

  /** The window can be made as narrow as the step where the path and toolbar give way. */
  @Test
  void theWindowGoesAsNarrowAsThePathAndToolbarGiveWay() throws IOException {
    Matcher narrow =
        Pattern.compile("(?m)^size (\\d+) (\\d+) +# narrow").matcher(Snaps.script("showcase"));
    assertTrue(narrow.find(), "showcase.txt has its narrow step");
    assertTrue(
        Showcase.MIN_WIDTH <= Integer.parseInt(narrow.group(1)),
        "a window of " + Showcase.MIN_WIDTH + " at least never shows what the step shows at "
            + narrow.group(1));
  }

  /**
   * Runs {@code showcase.txt} through every section, snapping only the ones it checks, since a
   * snap costs most of the time. Its snaps are not stored: they would change with every change of
   * the kit's look. The example's {@code main} with the script writes them all to look at.
   */
  @Test
  void theScriptShowsEverySection() throws IOException {
    Snaps.assumeText();
    Path out = Path.of("target", "showcase");
    String script =
        Snaps.script("showcase").replaceAll("(?m)^snap (?!(find|fields|contrast)\\b).*$", "");
    assertFalse(Script.run(script, Showcase.app(Look.DARK), out).isEmpty(), "the script snaps");
    assertTrue(
        Files.readString(out.resolve("find.tree.txt")).contains("role=search"),
        "the code editor's find bar");
    // Notes longer than their card's width wrap inside it instead of running out of it.
    assertWraps(out.resolve("fields.tree.txt"), "\"Line numbers, colors");
    assertWraps(out.resolve("contrast.tree.txt"), "\"Each cell is the pair itself");
  }

  @Test
  void startButtonKeepsItsWidthWhenBusy(@TempDir Path out) throws IOException {
    Snaps.assumeText();
    Path fixture = out.resolve("showcase-download-width");
    Files.createDirectories(fixture);
    App<Showcase> app = Showcase.app(Look.DARK);

    String readyScript = "size 1280 900 scale 1\nframe\n"
        + "press role=option label=\"Progress\"\nframe\nadvance 1\nframe";
    Script.run(readyScript, app, fixture);
    int readyWidth = startButtonWidth(app.tree().dump());

    String busyScript = "press #start\nframe\nadvance 1\nframe";
    Script.run(busyScript, app, fixture);
    int busyWidth = startButtonWidth(app.tree().dump());

    assertTrue(app.state().downloading, "state.downloading should be true after starting");
    assertTrue(app.tree().dump().contains("Downloading"), "busy tree should show Downloading text");
    assertEquals(readyWidth, busyWidth, "button width changed when busy");
  }

  private static int startButtonWidth(String tree) {
    String line = tree.lines()
        .filter(l -> l.contains("el #start role=button"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("start button not found"));
    Matcher size = Pattern.compile(" (-?\\d+),(-?\\d+) (\\d+)x(\\d+)").matcher(line);
    assertTrue(size.find(), "no coordinate in line: " + line);
    return Integer.parseInt(size.group(3));
  }

  /** The text whose line in the dump holds {@code start} is higher than one line of 13 pixels. */
  private static void assertWraps(Path dump, String start) throws IOException {
    String line = Files.readString(dump)
        .lines()
        .filter(l -> l.contains(start))
        .findFirst()
        .orElseThrow();
    Matcher size = Pattern.compile(" \\d+,\\d+ \\d+x(\\d+)").matcher(line);
    assertTrue(size.find(), line);
    assertTrue(Integer.parseInt(size.group(1)) > 20, "on one line: " + line);
  }
}
