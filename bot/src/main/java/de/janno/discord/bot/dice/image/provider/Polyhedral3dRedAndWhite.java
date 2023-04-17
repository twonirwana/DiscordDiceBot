package de.janno.discord.bot.dice.image.provider;

public class Polyhedral3dRedAndWhite extends PolyhedralFileImageProvider {
    @Override
    protected String getStyleFolder() {
        return "images/polyhedral_3d_red_and_white/";
    }

    @Override
    protected String getDieFolder(int totalDieSides) {
        return "d%d/".formatted(totalDieSides);
    }

    @Override
    protected String getFileName(int totalDieSides, int shownDieSide) {
        return "d%ds%d.png".formatted(totalDieSides, shownDieSide);
    }
}
