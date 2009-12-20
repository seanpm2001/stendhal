package games.stendhal.server.maps.quests;

import games.stendhal.common.Grammar;
import games.stendhal.server.core.engine.SingletonRepository;
import games.stendhal.server.entity.item.Item;
import games.stendhal.server.entity.item.StackableItem;
import games.stendhal.server.entity.npc.ChatAction;
import games.stendhal.server.entity.npc.ConversationPhrases;
import games.stendhal.server.entity.npc.ConversationStates;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.action.ExamineChatAction;
import games.stendhal.server.entity.npc.action.MultipleActions;
import games.stendhal.server.entity.npc.action.SetQuestAndModifyKarmaAction;
import games.stendhal.server.entity.npc.condition.AndCondition;
import games.stendhal.server.entity.npc.condition.NotCondition;
import games.stendhal.server.entity.npc.condition.QuestActiveCondition;
import games.stendhal.server.entity.npc.condition.QuestCompletedCondition;
import games.stendhal.server.entity.npc.condition.QuestInStateCondition;
import games.stendhal.server.entity.npc.parser.Sentence;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.util.ItemCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * QUEST: Herbs For Carmen
 * 
 * PARTICIPANTS:
 * <ul>
 * <li>Carmen (the healer in Semos)</li>
 * </ul>
 * 
 * STEPS:
 * <ul>
 * <li>Carmen introduces herself and asks for some items to help her heal people.</li>
 * <li>You collect the items.</li>
 * <li>Carmen sees yours items, asks for them then thanks you.</li>
 * </ul>
 * 
 * REWARD:
 * <ul>
 * <li>50 XP</li>
 * <li>2 antidote</li>
 * <li>Karma: 10</li>
 * </ul>
 * 
 * REPETITIONS:
 * <ul>
 * <li>None</li>
 * </ul>
 */
public class HerbsForCarmen extends AbstractQuest {

	public static final String QUEST_SLOT = "herbs_for_carmen";

	/**
	 * required items for the quest.
	 */
	protected static final String NEEDED_ITEMS = "arandula=5;porcini=1;apple=3;wood=2;button mushroom=1";

	@Override
	public List<String> getHistory(final Player player) {
		final List<String> res = new ArrayList<String>();
		if (!player.hasQuest(QUEST_SLOT)) {
			return res;
		}
		res.add("FIRST_CHAT");
		final String questState = player.getQuest(QUEST_SLOT);
		if ("rejected".equals(questState)) {
			res.add("QUEST_REJECTED");
		}
		if ("done".equals(questState)) {
			res.add("DONE");
		}
		return res;
	}

	private void prepareRequestingStep() {
		final SpeakerNPC npc = npcs.get("Carmen");

		npc.addInitChatMessage(null, new ChatAction() {
			public void fire(final Player player, final Sentence sentence, final SpeakerNPC engine) {
				if (!player.hasQuest("herbs_for_carmen")) {
					player.setQuest("herbs_for_carmen", "start");
					engine.listenTo(player, "hi");
				}
			}
			});

		npc.add(ConversationStates.IDLE, ConversationPhrases.GREETING_MESSAGES,
			new QuestInStateCondition(QUEST_SLOT,"start"),
			ConversationStates.QUESTION_1, 
			"Hey you! Yes, you! Do you know me?", null);

		npc.add(ConversationStates.IDLE, ConversationPhrases.GREETING_MESSAGES,
			new QuestInStateCondition(QUEST_SLOT,"rejected"),
			ConversationStates.QUEST_OFFERED, 
			"Hey, are you going to help me yet?", null);

		npc.add(
			ConversationStates.QUESTION_1,
			ConversationPhrases.YES_MESSAGES,
			new QuestInStateCondition(QUEST_SLOT,"start"),
			ConversationStates.ATTENDING,
			"Great, so you know my job. My supply of healing #ingredients is running low.",
			null);

		npc.add(
			ConversationStates.QUESTION_1,
			ConversationPhrases.NO_MESSAGES,
			new QuestInStateCondition(QUEST_SLOT,"start"),
			ConversationStates.ATTENDING,
			"I am Carmen. I can heal you for free, until your powers become too strong. Many warriors ask for my help. Now my #ingredients are running out and I need to fill up my supplies.",
			null);

		npc.add(
			ConversationStates.ATTENDING,
			"ingredients",
			new QuestInStateCondition(QUEST_SLOT,"start"),
			ConversationStates.QUEST_OFFERED,
			"So many people are asking me to heal them. That uses many ingredients and now my inventories are near empty. Can you help me to fill them up? ",
			null);

		npc.add(
			ConversationStates.QUEST_OFFERED,
			ConversationPhrases.YES_MESSAGES,
			null,
			ConversationStates.ATTENDING,
			null,
			new MultipleActions(new SetQuestAndModifyKarmaAction(QUEST_SLOT, NEEDED_ITEMS, 5.0),
								new ChatAction() {
									public void fire(final Player player, final Sentence sentence, final SpeakerNPC engine) {
										engine.say("Oh how nice. Please bring me those ingredients: " 
												   + Grammar.enumerateCollection(getMissingItems(player).toStringListWithHash()) + ".");
									}}));

		npc.add(
			ConversationStates.QUEST_OFFERED,
			ConversationPhrases.NO_MESSAGES,
			null,
			ConversationStates.ATTENDING,
			"Hargh, thats not good! But ok, its your choice. But remember, I will tell the others that I can't heal them much longer, because YOU didn't want to help me.",
			new SetQuestAndModifyKarmaAction(QUEST_SLOT, "rejected", -5.0));

		npc.add(
			ConversationStates.ATTENDING,
			"apple",
			null,
			ConversationStates.ATTENDING,
			"Apples have many vitamins, I saw some apple trees on the east of semos.",
			null);

		npc.add(
			ConversationStates.ATTENDING,
			"wood",
			null,
			ConversationStates.ATTENDING,
			"Wood is great resource with many different purposes. Of course you can find logs in a forest.",
			null);

		npc.add(
			ConversationStates.ATTENDING,
			Arrays.asList("button mushroom","porcini"),
			null,
			ConversationStates.ATTENDING,
			"Someone told me there are many different mushrooms in the Semos forest, follow the path south from here.",
			null);

		npc.add(
			ConversationStates.ATTENDING,
			"arandula",
			null,
			ConversationStates.ATTENDING,
			"North of Semos, near the tree grove, grows a herb called arandula. Here is a picture so you know what to look for.",
			new ExamineChatAction("arandula.png", "Carmen's drawing", "Arandula"));

	}

	private void prepareBringingStep() {
		final SpeakerNPC npc = npcs.get("Carmen");
	
		npc.add(ConversationStates.IDLE, ConversationPhrases.GREETING_MESSAGES,
				new AndCondition(new QuestActiveCondition(QUEST_SLOT),
							 new NotCondition(new QuestInStateCondition(QUEST_SLOT,"start")),
							 new NotCondition(new QuestInStateCondition(QUEST_SLOT,"rejected"))),
				ConversationStates.QUESTION_2,
				"Hi again. Did you bring me any #ingredients?",
				null);

		/* player asks what exactly is missing (says ingredients) */
		npc.add(ConversationStates.QUESTION_2, "ingredients", null,
				ConversationStates.QUESTION_2, null,
				new ChatAction() {
					public void fire(final Player player, final Sentence sentence, final SpeakerNPC engine) {
						final List<String> needed = getMissingItems(player).toStringListWithHash();
						engine.say("I need "
								+ Grammar.enumerateCollection(needed)
								+ ". Did you bring something?");
					}
				});

		/* player says he has a required item with him (says yes) */
		npc.add(ConversationStates.QUESTION_2,
				ConversationPhrases.YES_MESSAGES, null,
				ConversationStates.QUESTION_2, "Great, what did you bring?",
				null);

		/* create the ChatAction used for item triggers */
		final ChatAction itemsChatAction = new ChatAction() {
			public void fire(final Player player, final Sentence sentence, final SpeakerNPC engine) {
                final String item = sentence.getTriggerExpression().getNormalized();
			    ItemCollection missingItems = getMissingItems(player);
				final Integer missingCount = missingItems.get(item);

				if ((missingCount != null) && (missingCount > 0)) {
					if (dropItems(player, item, missingCount)) {
						missingItems = getMissingItems(player);

						if (missingItems.size() > 0) {
							engine.say("Good, do you have anything else?");
						} else {
							engine.say("Great! Now I can heal many people for free. Thanks a lot. Take this for your work.");
							player.setQuest(QUEST_SLOT, "done");
							final StackableItem reward = (StackableItem) SingletonRepository.getEntityManager().getItem("antidote");
							reward.setQuantity(2);
							player.equipOrPutOnGround(reward);
							player.addXP(50);
							player.notifyWorldAboutChanges();
							player.addKarma(5.0);
							engine.setCurrentState(ConversationStates.ATTENDING);
						}
					} else {
						engine.say("Oh, you don't have " + item + " with you!");
					}
				} else {
					engine.say("You have already brought that for me but thank you anyway.");
				}
			}
		};

		/* add triggers for the item names */
		final ItemCollection items = new ItemCollection();
		items.addFromQuestStateString(NEEDED_ITEMS);
		for (final Map.Entry<String, Integer> item : items.entrySet()) {
			npc.add(ConversationStates.QUESTION_2, item.getKey(), null,
					ConversationStates.QUESTION_2, null, itemsChatAction);
		}

		/* player says he didn't bring any items (says no) */
		npc.add(ConversationStates.ATTENDING, ConversationPhrases.NO_MESSAGES,
				new AndCondition(new QuestActiveCondition(QUEST_SLOT),
								 new NotCondition(new QuestInStateCondition(QUEST_SLOT,"start")),
								 new NotCondition(new QuestInStateCondition(QUEST_SLOT,"rejected"))),
				ConversationStates.ATTENDING,
				"Ok, well just let me know if I can #help you with anything else.", 
				null);

		/* player says he didn't bring any items to different question */
		npc.add(ConversationStates.QUESTION_2,
				ConversationPhrases.NO_MESSAGES,
				new AndCondition(new QuestActiveCondition(QUEST_SLOT),
								 new NotCondition(new QuestInStateCondition(QUEST_SLOT,"start")),
								 new NotCondition(new QuestInStateCondition(QUEST_SLOT,"rejected"))),
				ConversationStates.ATTENDING,
				"Ok, well just let me know if I can #help you with anything else.", null);

		npc.add(ConversationStates.IDLE, 
				ConversationPhrases.GREETING_MESSAGES,
				new QuestCompletedCondition(QUEST_SLOT),
				ConversationStates.ATTENDING, 
				"Hi, may I #help?", null);
		
	}
	
	/**
	 * Returns all items that the given player still has to bring to complete the quest.
	 *
	 * @param player The player doing the quest
	 * @return A list of item names
	 */
	private ItemCollection getMissingItems(final Player player) {
		final ItemCollection missingItems = new ItemCollection();

		missingItems.addFromQuestStateString(player.getQuest(QUEST_SLOT));

		return missingItems;
	}

	/**
	 * Drop specified amount of given item. If player doesn't have enough items,
	 * all carried ones will be dropped and number of missing items is updated.
	 *
	 * @param player
	 * @param itemName
	 * @param itemCount
	 * @return true if something was dropped
	 */
	private boolean dropItems(final Player player, final String itemName, int itemCount) {
		boolean result = false;

		 // parse the quest state into a list of still missing items
		final ItemCollection itemsTodo = new ItemCollection();

		itemsTodo.addFromQuestStateString(player.getQuest(QUEST_SLOT));

		if (player.drop(itemName, itemCount)) {
			if (itemsTodo.removeItem(itemName, itemCount)) {
				result = true;
			}
		} else {
			/*
			 * handle the cases the player has part of the items or all divided
			 * in different slots
			 */
			final List<Item> items = player.getAllEquipped(itemName);
			if (items != null) {
				for (final Item item : items) {
					final int quantity = item.getQuantity();
					final int n = Math.min(itemCount, quantity);

					if (player.drop(itemName, n)) {
						itemCount -= n;

						if (itemsTodo.removeItem(itemName, n)) {
							result = true;
						}
					}

					if (itemCount == 0) {
						result = true;
						break;
					}
				}
			}
		}

		 // update the quest state if some items are handed over
		if (result) {
			player.setQuest(QUEST_SLOT, itemsTodo.toStringForQuestState());
		}

		return result;
	}

	@Override
	public void addToWorld() {
		super.addToWorld();

		prepareRequestingStep();
		prepareBringingStep();
	}

	@Override
	public String getSlotName() {
		return QUEST_SLOT;
	}

	@Override
	public String getName() {
		return "HerbsForCarmen";
	}

	public String getTitle() {
		
		return "Herbs for Carmen";
	}
	

}
