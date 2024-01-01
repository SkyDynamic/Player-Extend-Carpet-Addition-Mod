package fengliu.peca.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.List;

public abstract class Page<Type> {
    protected final ServerCommandSource context;
    protected final List<Type> data;
    protected final int dataSize;
    protected final int limit = 5;
    protected int pageIn = 0;
    protected int pageCount;
    protected int pageDataIndex = 1;

    public Page(ServerCommandSource context, List<Type> data){
        this.context = context;
        this.data = data;
        this.dataSize = data.size();
        this.pageCount = (int) Math.ceil((double) data.size() / this.limit);
    }

    public List<Type> getPageData(){
        int offset = this.limit * this.pageIn;
        int toIndex = offset + this.limit;
        if (toIndex > this.dataSize){
            toIndex = this.dataSize;
        }
        return this.data.subList(offset, toIndex);
    }

    public abstract List<MutableText> putPageData(Type pageData, int index);

    public void look(){
        this.pageDataIndex = this.pageIn * this.limit + 1;
        this.context.sendFeedback(new TranslatableText("peca.info.page.count", String.format("%s/%s", this.pageIn + 1, this.pageCount)), false);
        this.getPageData().forEach(data -> {
            this.putPageData(data, this.pageDataIndex).forEach(msg -> this.context.sendFeedback(msg, false));
            this.pageDataIndex++;
        });
        this.context.sendFeedback(TextClickUtil.runText(new TranslatableText("peca.info.page.prev"), "/peca prev")
                .append(TextClickUtil.runText(new TranslatableText("peca.info.page.next"), "/peca next"))
                .append(TextClickUtil.suggestText(new TranslatableText("peca.info.page.to"), "/peca to ")), false);
    }

    public void next(){
        if (this.pageIn + 1 < this.pageCount){
            this.pageIn++;
        }

        this.look();
    }

    public void prev(){
        if (pageIn > 0){
            this.pageIn--;
        }

        this.look();
    }

    public void to(int toPage){
        if (this.pageIn + 1 < this.pageCount && toPage > 0){
            this.pageIn = toPage;
        }

        this.look();
    }
}
