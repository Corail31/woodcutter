package ovh.corail.woodcutter.command;

class WoodcuttingJsonRecipe {
    Conditions[] conditions;
    final String type = "corail_woodcutter:woodcutting";
    final Ingredient ingredient;
    final String result;
    final int count;

    WoodcuttingJsonRecipe(String ingredient, String result, int count, boolean isTag) {
        this.ingredient = new Ingredient(ingredient, isTag);
        this.result = result;
        this.count = count;
    }

    static class Ingredient {
        public String item;
        public String tag;

        Ingredient(String name, boolean isTag) {
            if (isTag) {
                this.tag = name;
            } else {
                this.item = name;
            }
        }
    }

    static class ConditionItem implements Conditions {
        final String type = "forge:item_exists";
        final String item;

        ConditionItem(String item) {
            this.item = item;
        }
    }

    static class ConditionMod implements Conditions {
        public final String type = "forge:mod_loaded";
        public final String modid;

        ConditionMod(String modid) {
            this.modid = modid;
        }
    }

    WoodcuttingJsonRecipe withConditions(Conditions... conditions) {
        this.conditions = conditions;
        return this;
    }

    interface Conditions {}
}
