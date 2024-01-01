package fengliu.peca;

import carpet.settings.Rule;

import static carpet.settings.RuleCategory.COMMAND;

public class PecaSettings {

    public static final String PECA = "PECA";

    @Rule(desc = "", category = {PECA})
    public static boolean groupCanBePlayerLogInSpawn = false;

    @Rule(desc = "", category = {PECA, COMMAND})
    public static String commandPlayerGroup = "ops";

    @Rule(desc = "", category = {PECA, COMMAND})
    public static String commandPlayerAuto = "ops";

    @Rule(desc = "", category = {PECA, COMMAND})
    public static String commandPlayerManage = "ops";

//    @Rule(category = {PECA, COMMAND})
//    public static String commandPlayerGroupManage = "ops";

    @Rule(desc = "", category = {PECA, COMMAND})
    public static boolean hiddenFakePlayerGroupJoinMessage = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerGameModeLockSurvive = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerCanWalkOnFluid = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerCanNotPickUpItem = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerCanNotAssimilateExp = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerCanNotSurroundExp = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerImmuneInFireDamage = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerImmuneOnFireDamage = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerImmuneLavaDamage = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerImmuneDamage = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerImmunePlayerDamage = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerImmuneExplosionDamage = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerImmuneCrammingDamage = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerNotDie = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerNotHunger = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerNotHypoxic = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerNotBeCaughtInPowderSnow = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerWillNotAffectedByBubbleColumn = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerDropAllExp = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerDropExpNoUpperLimit = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerKeepInventory = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerReplaceLowTool = false;

    @Rule(desc = "", category = {PECA})
    public static boolean fakePlayerDropLowTool = false;
}
