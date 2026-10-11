package com.example.mindshift.data

data class WorksheetField(
    val id: String,
    val label: String,
    val hint: String = "",
    val minLines: Int = 3
)

data class WorksheetDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val fields: List<WorksheetField>
)

object Worksheets {

    // ---------------- CBT ----------------

    val thoughtRecord = WorksheetDefinition(
        id = "thought_record",
        title = "Thought Record",
        subtitle = "Catch, check, and change a difficult thought",
        emoji = "📝",
        fields = listOf(
            WorksheetField("situation", "1. The situation", "What happened? Stick to facts — where, when, who.", 2),
            WorksheetField("emotions", "2. Emotions & intensity", "What did you feel (sad, anxious, angry…)? Rate each 0–100%.", 2),
            WorksheetField("auto_thought", "3. Automatic thought", "What went through your mind? What did it say about you, others, or the future?", 3),
            WorksheetField("evidence_for", "4. Evidence for", "What facts support this thought?", 2),
            WorksheetField("evidence_against", "5. Evidence against", "What facts don't support it? What would you tell a friend who thought this?", 2),
            WorksheetField("balanced", "6. Balanced thought", "Write a fairer, more realistic way to see the situation.", 3),
            WorksheetField("rerate", "7. Re-rate emotions", "How intense are the emotions now (0–100%)?", 2)
        )
    )

    val distortionCheck = WorksheetDefinition(
        id = "distortion_check",
        title = "Thinking Trap Check",
        subtitle = "Name the trap and loosen its grip",
        emoji = "🔍",
        fields = listOf(
            WorksheetField("situation", "The situation", "Briefly: what set the thought off?", 2),
            WorksheetField("thought", "The thought", "Word for word, as it showed up.", 2),
            WorksheetField("belief_before", "Belief rating", "How much do you believe it right now? 0–100%.", 1),
            WorksheetField("trap", "The trap", "Which thinking trap(s)? See the list on this tab.", 2),
            WorksheetField("reframe", "The reframe", "What's a more accurate, kinder way to see it? What would you tell a friend?", 3),
            WorksheetField("belief_after", "Belief now", "Re-rate: 0–100%.", 1)
        )
    )

    val behavioralActivation = WorksheetDefinition(
        id = "behavioral_activation",
        title = "Action Before Motivation",
        subtitle = "Gently move toward what you've been avoiding",
        emoji = "🌤️",
        fields = listOf(
            WorksheetField("avoiding", "What I've been avoiding", "A task, place, or person.", 2),
            WorksheetField("feeling", "The feeling underneath", "What emotion am I avoiding feeling?", 2),
            WorksheetField("first_step", "Smallest first step", "So small it's almost silly. What is it?", 2),
            WorksheetField("when_where", "When & where", "Day, time, place — make it concrete.", 2),
            WorksheetField("obstacle", "Obstacle plan", "What might get in the way, and what I'll do then.", 2),
            WorksheetField("afterward", "Afterward", "Fill in later: what actually happened? How do I feel now?", 2)
        )
    )

    val downwardArrow = WorksheetDefinition(
        id = "downward_arrow",
        title = "Downward Arrow",
        subtitle = "Follow a thought down to the belief underneath it",
        emoji = "⬇️",
        fields = listOf(
            WorksheetField("start", "The starting thought", "The one that keeps coming back.", 2),
            WorksheetField("level1", "And if that were true, what would it mean?", "Don't argue with it — just answer as if it were true.", 2),
            WorksheetField("level2", "And what would THAT mean?", "Follow it one level further down.", 2),
            WorksheetField("level3", "And if that were true?", "Usually three or four arrows reach the bottom.", 2),
            WorksheetField("core", "The belief at the bottom", "Core beliefs tend to sound like: \"I'm not good enough.\" \"I'm unlovable.\" \"I'm not safe.\" \"I'm trapped.\"", 3),
            WorksheetField("evidence", "What this belief ignores", "Times you've contradicted it — moments that don't fit, people who'd disagree, things you've survived.", 3),
            WorksheetField("fairer", "A fairer bottom line", "Same honesty, full picture: \"Sometimes I struggle AND I've handled hard things before.\"", 3)
        )
    )

    val thoughtCourt = WorksheetDefinition(
        id = "thought_court",
        title = "Thought Court",
        subtitle = "Put the thought on trial instead of taking its word for it",
        emoji = "🏛️",
        fields = listOf(
            WorksheetField("charge", "The thought on trial", "State it exactly, like a charge being read: \"I always ruin everything.\"", 2),
            WorksheetField("for", "The case FOR", "Every piece of evidence that seems to support it. Don't argue — just list what the prosecutor has.", 3),
            WorksheetField("against", "The case AGAINST", "Facts that don't fit the charge: times it wasn't true, details left out, what a fair witness would add.", 3),
            WorksheetField("cross", "Cross-examination", "Find the weak points: absolute words (always, never, everyone)? Mind-reading? Name-calling dressed as fact?", 3),
            WorksheetField("verdict", "The verdict", "Rule like a fair judge — not let off easy, not the maximum sentence. What actually happened?", 3),
            WorksheetField("rerate", "How true does it feel now?", "Compare to before the trial. Beliefs move slowly — even 10% matters.", 2)
        )
    )

    val worryTree = WorksheetDefinition(
        id = "worry_tree",
        title = "The Worry Tree",
        subtitle = "Sort every worry: do something, schedule it, or set it down",
        emoji = "🌳",
        fields = listOf(
            WorksheetField("worry", "The worry", "Write the \"what if\" plainly.", 2),
            WorksheetField("action_possible", "Can I do something about it today?", "Honest answer: is there any action available — me, today?", 2),
            WorksheetField("plan", "If yes — the plan", "What, exactly, and when? Vague plans feed worry; scheduled ones end it.", 2),
            WorksheetField("letgo", "If no — how I'll set it down", "Postpone it: pick a 15-minute \"worry window\" later today, and name what you'll turn toward until then.", 2),
            WorksheetField("window", "At the worry window", "If it still matters when the window comes, make the plan above. Most worries don't show up for their appointment.", 2),
            WorksheetField("reflect", "Afterward", "What did sorting it — instead of circling it — change?", 2)
        )
    )

    val behavioralExperiment = WorksheetDefinition(
        id = "behavioral_experiment",
        title = "Behavioral Experiment",
        subtitle = "Test a feared prediction in real life — then trust the data",
        emoji = "🧪",
        fields = listOf(
            WorksheetField("prediction", "The prediction", "Specific and testable: \"If I speak up in the meeting, everyone will think I'm incompetent.\"", 2),
            WorksheetField("belief", "How much I believe it", "0–100%. You'll re-rate this later.", 1),
            WorksheetField("test", "The test", "A small, safe way to check it in the real world. What exactly will you do, where, and when?", 3),
            WorksheetField("criteria", "What each result would mean", "Decide before you run it: what outcome would support the prediction? What would count as evidence against it?", 2),
            WorksheetField("result", "The result", "Fill in afterward: what actually happened — including what you felt, not just what occurred.", 3),
            WorksheetField("verdict", "The verdict", "Re-rate the belief 0–100%. What's the most accurate thing you can now say about the prediction?", 2)
        )
    )

    val pleasureMastery = WorksheetDefinition(
        id = "pleasure_mastery",
        title = "Pleasure & Mastery",
        subtitle = "Balance the day between what feels good and what builds you",
        emoji = "🎯",
        fields = listOf(
            WorksheetField("plan", "Today's mix", "Pick one thing for pleasure (just fun) and one for mastery (a little effortful, done = proud). They can be tiny.", 3),
            WorksheetField("pleasure", "Pleasure ratings", "Afterward: rate each activity 0–10 for pleasure. Be curious, not strict.", 2),
            WorksheetField("mastery", "Mastery ratings", "Afterward: rate 0–10 for mastery — difficulty met, competence felt.", 2),
            WorksheetField("notice", "What I noticed", "Which gave more than expected? Low-mood days under-predict pleasure. What did the data say?", 2),
            WorksheetField("tomorrow", "Tomorrow's mix", "One pleasure + one mastery, planned now while it's fresh.", 2)
        )
    )

    // ---------------- DBT ----------------

    val dearMan = WorksheetDefinition(
        id = "dear_man",
        title = "DEAR MAN",
        subtitle = "A script for asking for what you need",
        emoji = "💬",
        fields = listOf(
            WorksheetField("describe", "D — Describe", "Just the facts: \"When you…\" / \"The plan was…\"", 2),
            WorksheetField("express", "E — Express", "\"I feel…\" — own the feeling without blaming.", 2),
            WorksheetField("assert", "A — Assert", "Ask directly or say no: \"I'd like…\" / \"I can't…\"", 2),
            WorksheetField("reinforce", "R — Reinforce", "What's in it for them? \"If this works out, then…\"", 2),
            WorksheetField("mindful", "M — Mindful", "If they deflect, what line will you calmly repeat?", 2),
            WorksheetField("confident", "A — Appear confident", "Note to self: tone, posture, eye contact — no apologizing for asking.", 2),
            WorksheetField("negotiate", "N — Negotiate", "What are you willing to give or trade?", 2)
        )
    )

    val tippPlan = WorksheetDefinition(
        id = "tipp_plan",
        title = "TIPP — Crisis Reset",
        subtitle = "Body-first tools to bring intense emotion down fast",
        emoji = "🧊",
        fields = listOf(
            WorksheetField("warning", "My warning signs", "How I know I'm at crisis level (racing heart, tunnel vision…).", 2),
            WorksheetField("temperature", "T — Temperature", "Cold water: ice on face, cold shower, cold can against the eyes. My plan:", 2),
            WorksheetField("exercise", "I — Intense exercise", "10–20 minutes to match the adrenaline: sprint, stairs, jumping jacks. What I'll do:", 2),
            WorksheetField("breathing", "P — Paced breathing", "In for 4, out for 6–8. Where and when I'll practice:", 2),
            WorksheetField("muscles", "P — Paired muscle relaxation", "Tense 5–10 seconds, then release with a long exhale, muscle group by group.", 2),
            WorksheetField("after", "After TIPP", "Once I've come down, my next step is… (rest, call someone, do a worksheet)", 2)
        )
    )

    val prosCons = WorksheetDefinition(
        id = "pros_cons",
        title = "Pros & Cons",
        subtitle = "Weigh it out with Wise Mind",
        emoji = "⚖️",
        fields = listOf(
            WorksheetField("urge", "The urge or situation", "", 2),
            WorksheetField("pros_tolerate", "Pros of NOT acting on the urge", "", 2),
            WorksheetField("cons_tolerate", "Cons of NOT acting on the urge", "", 2),
            WorksheetField("pros_givein", "Pros of giving in", "", 2),
            WorksheetField("cons_givein", "Cons of giving in", "", 2),
            WorksheetField("decision", "My Wise Mind decision", "Looking at all four boxes — what do I choose?", 3)
        )
    )

    val oppositeAction = WorksheetDefinition(
        id = "opposite_action",
        title = "Opposite Action",
        subtitle = "Act against the emotion when it isn't serving you",
        emoji = "🔄",
        fields = listOf(
            WorksheetField("emotion", "The emotion", "Name it: fear, anger, sadness, shame, guilt…", 1),
            WorksheetField("urge", "The action urge", "What does it want me to do — hide, snap, avoid, numb out?", 2),
            WorksheetField("facts", "Check the facts", "Does the emotion fit the facts? Is its intensity warranted?", 2),
            WorksheetField("opposite", "The opposite action", "If it doesn't fit: do the opposite, all the way. What will I do?", 2),
            WorksheetField("commit", "Full commitment", "Body language, tone, and mind — what does 'all the way' look like?", 2)
        )
    )

    val rideTheWave = WorksheetDefinition(
        id = "ride_the_wave",
        title = "Ride the Wave",
        subtitle = "Urges rise, peak, and fall — surf it instead of fighting it",
        emoji = "🌊",
        fields = listOf(
            WorksheetField("urge", "The urge", "What is it pushing me to do?", 2),
            WorksheetField("intensity_now", "Intensity now", "Rate it 0–100.", 1),
            WorksheetField("body", "Where I feel it", "Scan your body — chest, hands, jaw, stomach. What's happening there?", 2),
            WorksheetField("ride", "Riding it", "Don't fight it, don't feed it. Watch it like weather. What do you notice after a few minutes?", 3),
            WorksheetField("intensity_after", "Intensity after a while", "Re-rate 0–100. Waves always crest.", 1),
            WorksheetField("remember", "What I'll remember", "What helped it pass? What works next time?", 2)
        )
    )

    val selfSoothe = WorksheetDefinition(
        id = "self_soothe",
        title = "Self-Soothing — 5 Senses",
        subtitle = "Comfort yourself through sight, sound, smell, taste, and touch",
        emoji = "🕯️",
        fields = listOf(
            WorksheetField("sight", "👀 Sight", "Something beautiful or calming — photos, nature, candlelight.", 2),
            WorksheetField("sound", "👂 Sound", "Music, nature sounds, a voice that soothes you.", 2),
            WorksheetField("smell", "👃 Smell", "Coffee, lavender, fresh air, a favorite lotion.", 2),
            WorksheetField("taste", "👄 Taste", "Tea, chocolate, something slow and savored.", 2),
            WorksheetField("touch", "🤲 Touch", "A soft blanket, a warm shower, a pet, a cozy sweater.", 2),
            WorksheetField("kit", "My soothing kit", "Which of these will I keep ready for hard moments?", 2)
        )
    )

    val checkFacts = WorksheetDefinition(
        id = "check_the_facts",
        title = "Check the Facts",
        subtitle = "Does the emotion fit what actually happened?",
        emoji = "🔎",
        fields = listOf(
            WorksheetField("event", "The event", "Only what a camera would record.", 2),
            WorksheetField("interpretation", "My interpretation", "What I assumed it meant.", 2),
            WorksheetField("threat", "Threat check", "Is there a real threat? How likely? How bad? Could I cope?", 3),
            WorksheetField("fit", "Does the emotion fit?", "Is the type and intensity of my emotion justified by the facts?", 2),
            WorksheetField("next", "Next step", "If it fits: problem-solve or act. If not: opposite action, or let it pass.", 3)
        )
    )

    val radicalAcceptance = WorksheetDefinition(
        id = "radical_acceptance",
        title = "Radical Acceptance",
        subtitle = "End the war with reality — acceptance is not approval",
        emoji = "🕊️",
        fields = listOf(
            WorksheetField("reality", "The reality I'm fighting", "What has already happened that I can't change? State it as fact.", 2),
            WorksheetField("cost", "What fighting it costs", "Rumination, tension, anger at the unchangeable — what does resistance take from me, hour by hour?", 2),
            WorksheetField("not", "What acceptance is NOT", "Not approval. Not liking it. Not giving up. It's seeing what is, so energy can go to what's next.", 2),
            WorksheetField("statements", "Accepting statements", "Try writing and re-reading: \"This is how it happened. It couldn't have gone otherwise, given everything. I can still build what comes next.\"", 3),
            WorksheetField("turnmind", "Turning the mind", "Acceptance isn't chosen once — it's chosen daily, sometimes hourly. What will remind me to turn again when I catch myself fighting?", 2),
            WorksheetField("agency", "What I can still do", "Accepting reality frees action, it doesn't replace it. What is within my power this week?", 2)
        )
    )

    val wiseMind = WorksheetDefinition(
        id = "wise_mind",
        title = "Wise Mind",
        subtitle = "The blend of feeling and thinking — where your best decisions live",
        emoji = "🧭",
        fields = listOf(
            WorksheetField("decision", "The decision", "The question you keep circling.", 2),
            WorksheetField("emotion", "Emotion Mind says", "Pure feeling, unfiltered: what does the heat of it want to do?", 2),
            WorksheetField("reason", "Reason Mind says", "Cold logic, no feelings in the room: what does the spreadsheet say?", 2),
            WorksheetField("overlap", "Where they agree", "Wise Mind is the overlap. What point do both the feeling and the logic make, in different languages?", 2),
            WorksheetField("answer", "My Wise Mind answer", "Often it arrives quieter than either voice. What do you know, beneath both?", 2),
            WorksheetField("check", "Gut check", "Sit quietly or sleep on it, then re-read your answer. Does it still ring true in your body?", 2)
        )
    )

    val copeAhead = WorksheetDefinition(
        id = "cope_ahead",
        title = "Cope Ahead",
        subtitle = "Rehearse a hard moment now, so it meets a prepared you",
        emoji = "🗺️",
        fields = listOf(
            WorksheetField("situation", "The situation ahead", "What's coming that might be hard — a conversation, event, or anniversary?", 2),
            WorksheetField("emotions", "The emotions I expect", "Name them, and what might set them off specifically.", 2),
            WorksheetField("fear", "The worst-case reel", "What does your mind show you when it plays this forward? Write it down — naming it shrinks it.", 2),
            WorksheetField("plan", "My coping plan", "Name the specific skills, in order: \"long exhales on the way there · one DEAR MAN line · leave by 9 if I need to.\"", 3),
            WorksheetField("rehearse", "Rehearsal", "Now run it in your mind like film: the situation, the trigger, your skill, yourself getting through it. Twice.", 3),
            WorksheetField("after", "Afterward", "Fill in later: how close was the reel to reality? Which skill earned its place?", 2)
        )
    )

    val giveSkills = WorksheetDefinition(
        id = "give_skills",
        title = "GIVE",
        subtitle = "Keep the relationship warm while you ask or say something hard",
        emoji = "🎁",
        fields = listOf(
            WorksheetField("goal", "The interaction", "Who is it with — and how do you want to feel about the relationship when it's over?", 2),
            WorksheetField("gentle", "G — Gentle", "No attacks, threats, or judging. What's the softest honest opening line?", 2),
            WorksheetField("interested", "I — Interested", "What will you ask, and how will you show you're listening? Plan to hear their side before you push yours.", 2),
            WorksheetField("validate", "V — Validate", "What's true or understandable from their side? Say it out loud — one sentence.", 2),
            WorksheetField("easy", "E — Easy manner", "Where can you be light, warm, even a little funny? A smile buys more than a speech.", 2),
            WorksheetField("after", "After", "How did it go — and what did keeping the relationship in mind change?", 2)
        )
    )

    val haltCheck = WorksheetDefinition(
        id = "halt_check",
        title = "HALT Check",
        subtitle = "Hungry · Angry · Lonely · Tired — tend the body before treating the feeling",
        emoji = "🛑",
        fields = listOf(
            WorksheetField("hungry", "Hungry?", "When did you last eat — really eat? Could hunger be doing some of the talking?", 2),
            WorksheetField("angry", "Angry or anxious?", "Is there a feeling you haven't named yet? Naming it is half of handling it.", 2),
            WorksheetField("lonely", "Lonely?", "Have you connected with anyone today, even briefly? A message counts.", 2),
            WorksheetField("tired", "Tired?", "How's your sleep? Sometimes the answer isn't insight — it's rest.", 2),
            WorksheetField("fix", "One thing to fix first", "Pick the loudest unmet need. What will you do for it in the next ten minutes?", 2),
            WorksheetField("recheck", "Re-check", "After tending to your body, look at the original problem again. Does it look the same size?", 2)
        )
    )

    val improveMoment = WorksheetDefinition(
        id = "improve_moment",
        title = "IMPROVE the Moment",
        subtitle = "A toolkit for the minutes you just have to get through",
        emoji = "✨",
        fields = listOf(
            WorksheetField("now", "What needs improving right now", "One sentence. The moment, not your whole life.", 2),
            WorksheetField("imagery", "I — Imagery", "Go somewhere in your mind: a beach, a kitchen, a memory that holds you. Build it in detail for a minute.", 2),
            WorksheetField("meaning", "M — Meaning", "Find or make meaning: what could this hard moment teach? What would you tell someone else about it?", 2),
            WorksheetField("prayer", "P — Prayer or presence", "However you frame it — prayer, meditation, connection to something larger — lean in for a moment.", 2),
            WorksheetField("relax", "R — Relaxation", "What unknots your body: slow breath, a shower, music, unclenching your jaw and shoulders?", 2),
            WorksheetField("one", "O — One thing at a time", "Just the next small step — not the whole day. What is it?", 2),
            WorksheetField("vacation", "V — Vacation", "A brief, healthy break: one walk, one song, five minutes of air. What's yours?", 2),
            WorksheetField("encourage", "E — Encouragement", "Cheer yourself like you'd cheer someone you love: what's the sentence you need to hear?", 2)
        )
    )

    val fastSkills = WorksheetDefinition(
        id = "fast_skills",
        title = "FAST",
        subtitle = "Keep your self-respect while asking or saying no",
        emoji = "🎖️",
        fields = listOf(
            WorksheetField("situation", "The interaction", "Where you need to hold your ground without losing your cool.", 2),
            WorksheetField("fair", "F — Fair to yourself (and them)", "You're allowed to want things. What do YOU actually want here — stated plainly?", 2),
            WorksheetField("apologies", "A — No over-apologizing", "One apology is honest; ten is surrender. What do you keep apologizing for that doesn't need it?", 2),
            WorksheetField("values", "S — Stick to your values", "Where's the line you won't cross to keep the peace — even if they frown?", 2),
            WorksheetField("truthful", "T — Truthful", "No exaggerating, no pretending, no white lies to smooth it over. What's the true version you've been softening?", 2),
            WorksheetField("after", "After", "How did it feel to hold your ground — and what did it teach you about doing it again?", 2)
        )
    )

    // ---------------- Mindfulness ----------------

    val grounding = WorksheetDefinition(
        id = "grounding_54321",
        title = "5-4-3-2-1 Grounding",
        subtitle = "Come back to the room through your senses",
        emoji = "🌱",
        fields = listOf(
            WorksheetField("see", "5 things I can see", "Look around slowly — colors, shapes, light.", 2),
            WorksheetField("feel", "4 things I can feel", "Feet on the floor, fabric, air on skin.", 2),
            WorksheetField("hear", "3 things I can hear", "Near and far. Let the sounds come to you.", 2),
            WorksheetField("smell", "2 things I can smell", "", 1),
            WorksheetField("taste", "1 thing I can taste", "", 1),
            WorksheetField("now", "How I feel now", "One sentence, no judgment.", 2)
        )
    )

    val gratitude = WorksheetDefinition(
        id = "gratitude",
        title = "Gratitude Journal",
        subtitle = "Train attention toward what's going right",
        emoji = "🙏",
        fields = listOf(
            WorksheetField("three", "Three good things today", "", 3),
            WorksheetField("why", "Why each one mattered", "", 3),
            WorksheetField("person", "Someone I appreciate", "Who, and what they did.", 2),
            WorksheetField("forward", "Paying it forward", "One small way I'll pass it on.", 2)
        )
    )

    val eveningWindDown = WorksheetDefinition(
        id = "evening_wind_down",
        title = "Evening Wind-Down",
        subtitle = "Close the day gently and set up tomorrow",
        emoji = "🌙",
        fields = listOf(
            WorksheetField("day", "Today in one line", "", 2),
            WorksheetField("good", "A moment I appreciated", "", 2),
            WorksheetField("hard", "Something hard, and how I met it", "", 2),
            WorksheetField("kindness", "One kind thing for myself tonight", "", 2),
            WorksheetField("tomorrow", "Tomorrow's first step", "Small and concrete.", 2)
        )
    )

    val selfCompassion = WorksheetDefinition(
        id = "self_compassion",
        title = "Self-Compassion Break",
        subtitle = "Three sentences for a hard moment — mindfulness, humanity, kindness",
        emoji = "🫶",
        fields = listOf(
            WorksheetField("moment", "The hard moment", "What's hurting right now? Say it plainly, without minimizing or inflating.", 2),
            WorksheetField("mindfulness", "1 · Mindfulness", "\"This is a moment of suffering. This hurts.\" Name what you feel without judging it for being there.", 2),
            WorksheetField("humanity", "2 · Common humanity", "\"Other people feel this too. I'm not broken or alone in this.\" What would remind you that you're in good company?", 2),
            WorksheetField("kindness", "3 · Kindness", "Hand on heart. What would you say to someone you love feeling exactly this? Say that to yourself.", 3),
            WorksheetField("next", "One kind action", "A cup of tea, a walk, rest, forgiveness. What will you actually do for yourself in the next hour?", 2)
        )
    )

    val bestPossibleSelf = WorksheetDefinition(
        id = "best_possible_self",
        title = "Best Possible Self",
        subtitle = "Write the future where things go well — clarity, not fantasy",
        emoji = "🌅",
        fields = listOf(
            WorksheetField("five_years", "Five years from now", "Imagine life went as well as it realistically could. Write it like a page from a diary dated five years out.", 4),
            WorksheetField("people", "The people", "Who's close? What are those relationships like — and what did you do to build them?", 2),
            WorksheetField("work", "Work & growth", "What are you doing, learning, becoming? What used to be hard that's easy now?", 2),
            WorksheetField("health", "Body & mind", "How do you live in this future — sleep, movement, the way you speak to yourself?", 2),
            WorksheetField("path", "The bridge to it", "One concrete thing from that future you can start this week. This exercise works when it points back at today.", 2)
        )
    )

    val valuesCompass = WorksheetDefinition(
        id = "values_compass",
        title = "Values Compass",
        subtitle = "Check the direction, not the speed",
        emoji = "🌟",
        fields = listOf(
            WorksheetField("values", "What matters most", "Name two or three values — the qualities you want your life to move toward (honesty, creativity, courage, care…).", 2),
            WorksheetField("alignment", "Where I'm aligned", "When did I last act on each one? Even small moments count — name them.", 2),
            WorksheetField("drift", "Where I've drifted", "Where has life been pulling me away from what matters? No blame — just an honest reading.", 2),
            WorksheetField("action", "One value-serving act", "This week: one small action one of your values would want. What is it?", 2)
        )
    )

    // ---------------- Special (Home-surfaced; not in tabs) ----------------

    val safetyPlan = WorksheetDefinition(
        id = "safety_plan",
        title = "My Safety Plan",
        subtitle = "A personal plan for the hardest moments — best written when you're steady, and shared with someone you trust",
        emoji = "🛟",
        fields = listOf(
            WorksheetField("warning", "1. My warning signs", "Thoughts, moods, or behaviors that tell me a hard stretch is starting.", 3),
            WorksheetField("coping", "2. Things I can do on my own", "Skills that have helped before: breathing, walking, music, shower, coping cards.", 3),
            WorksheetField("distraction", "3. People & places that take the edge off", "Who can I be around, or where can I go, without having to explain anything?", 2),
            WorksheetField("people", "4. People I can ask for help", "Names and numbers. It's okay to copy them straight from your contacts.", 3),
            WorksheetField("professionals", "5. Professionals & crisis lines", "My therapist / doctor. Crisis lines: 988 call or text (US) · findahelpline.com worldwide.", 2),
            WorksheetField("space", "6. Making my space safer", "Small steps that put time and distance between me and means of harm — who can hold what, what to lock away.", 2),
            WorksheetField("reasons", "7. My reasons for living", "People, pets, hopes, plans, places. Specific beats general.", 3)
        )
    )

    val weeklyReflection = WorksheetDefinition(
        id = "weekly_reflection",
        title = "Weekly Reflection",
        subtitle = "Ten honest minutes on the week — patterns, wins, and one thing to try next",
        emoji = "🪞",
        fields = listOf(
            WorksheetField("highlights", "Moments that stand out", "Good, bad, or just vivid.", 3),
            WorksheetField("patterns", "Patterns I notice", "In mood, thoughts, sleep, situations — anything repeating?", 3),
            WorksheetField("helped", "What helped", "Which tools or people moved the needle?", 2),
            WorksheetField("hard", "What was hard", "And what I want to remember about getting through it.", 3),
            WorksheetField("next", "One thing I'll try next week", "Small, specific, doable.", 2),
            WorksheetField("kind", "A kind note to myself", "What I'd say to someone who had my week.", 2)
        )
    )

    val imaginalExposure = WorksheetDefinition(
        id = "imaginal_exposure",
        title = "Imaginal Exposure",
        subtitle = "For fears you can't safely rehearse in real life — work with the image, gently, in bounded sessions",
        emoji = "🌫️",
        fields = listOf(
            WorksheetField("what", "The fear or memory", "Name what you're working with — and why rehearsing it in real life isn't safe or appropriate right now.", 2),
            WorksheetField("readiness", "Steadiness check — do this first", "Am I rested, grounded, with support around? If I'm at crisis level, TIPP comes first. And if this traces back to trauma, this work belongs WITH a therapist — bring this worksheet to a session.", 3),
            WorksheetField("stop", "My stop signals", "How I'll know to stop: flooding, numbness, losing time or place, urges to act on the fear. When they show up: eyes open, feet on the floor, 5-4-3-2-1, done for today. Stopping early is skill, not failure.", 3),
            WorksheetField("scene", "The scene, in present tense", "First person, present tense, sensory: \"I am standing in… I see… I hear…\" One scene only — this is the script you'll return to each session.", 5),
            WorksheetField("before", "SUDS before (0–100)", "", 1),
            WorksheetField("during", "The session", "Set a timer — 5 to 15 minutes, no longer — and stay with the image, eyes closed, letting the fear rise, crest, and fall without looking away. Afterwards: what happened to it while you stayed?", 3),
            WorksheetField("after", "SUDS after (0–100)", "", 1),
            WorksheetField("close", "Landing", "Come back gently: eyes open, name the room, one long exhale. Then one kind thing — tea, a walk, a message to someone safe. Imaginal work deserves an aftercare plan like any exposure.", 2),
            WorksheetField("next", "Next session", "Same scene, again, until it bores you — repetition is the mechanism, not a sign it isn't working. When it's flat: add the next scene, or close the ladder.", 2)
        )
    )

    // ---------------- Registries ----------------

    val cbt = listOf(
        thoughtRecord, distortionCheck, behavioralActivation,
        downwardArrow, thoughtCourt, worryTree, behavioralExperiment,
        pleasureMastery
    )
    val dbt = listOf(
        dearMan, tippPlan, prosCons, oppositeAction, rideTheWave, selfSoothe, checkFacts,
        radicalAcceptance, wiseMind, copeAhead, giveSkills, haltCheck, improveMoment,
        fastSkills
    )
    val mindfulness = listOf(
        grounding, gratitude, eveningWindDown,
        selfCompassion, bestPossibleSelf, valuesCompass
    )
    val special = listOf(safetyPlan, weeklyReflection, imaginalExposure)

    val all: List<WorksheetDefinition> get() = cbt + dbt + mindfulness + special

    fun byId(id: String): WorksheetDefinition? = all.firstOrNull { it.id == id }
}
