package ovh.corail.woodcutter.command;

class WoodcuttingJsonRecipe {
    public Conditions[] conditions;
    private final String type = "corail_woodcutter:woodcutting";
    public final Ingredient ingredient;
    public final String result;
    public final int count;

    WoodcuttingJsonRecipe(String ingredient, String result, int count, boolean isTag) {
        this.ingredient = new Ingredient(ingredient, isTag);
        this.result = result;
        this.count = count;
    }

    class Ingredient {
        protected String item;
        protected String tag;

        Ingredient(String name, boolean isTag) {
            if (isTag) {
                this.tag = name;
            } else {
                this.item = name;
            }
        }
    }

    static class ConditionItem implements Conditions {
        private final String type = "forge:item_exists";
        private final String item;

        ConditionItem(String item) {
            this.item = item;
        }
    }

    static class ConditionMod implements Conditions {
        private final String type = "forge:mod_loaded";
        private final String modid;

        ConditionMod(String modid) {
            this.modid = modid;
        }
    }

    interface Conditions {}
}
