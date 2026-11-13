# Family weather: "will it rain when I leave for work on Monday?"

Status: **decisions taken (13 November 2026), building starts with W1**. Section 9 records the owner's answers;
section 10 marks which ideas were accepted.

## 1. The idea in one paragraph

The app learns (or is told) the family's weekly routine: who is where on which weekday and when ("Monday, I leave for
work at 8:00", "Leo is at school 7:40 to 13:00"). It then asks a weather service for the forecast **at those places
and times**, not for "the city", and says only what matters: "Monday 8:00: rain, 70 %: take an umbrella", "Wednesday
school run: 3 degrees, frost". The weather comes to the person at the moment it is useful (the evening before, or in
the morning), shaped by their own routine.

## 2. What the app already has (so what is new)

| Have today | Where | Useful for weather |
|---|---|---|
| Named places (Home, School...) with a centre and radius | `Place`, `places` collection, `:feature:map` | where to ask the forecast for |
| Arrival and leaving events at those places | `PlaceEvent`, `PresenceTracker`, `placeEvents` (kept only for a short time, old ones are deleted) | the raw signal of the routine |
| Background position, now also frequent while moving | `:core:location`, `LiveShareService` | detecting trips |
| Repeating chores with weekdays and times | `Chore`, `Recurrence` | a second kind of routine, and chores that depend on weather |
| Per-phone notification settings, quiet hours | `:core:settings` | where the weather alert is switched on or off |

New: a **routine model** (weekday + time + from + to), a **weather client**, **advice rules** (umbrella, jacket, frost...)
and the places where they are shown.

## 3. How the routine is known

Three sources, from simplest to most ambitious. They can be combined.

1. **Told (manual).** "Weekly plan" editor: pick people, weekdays, a time, a place ("Mon to Fri, 8:00, Work").
   No tracking at all, so it also works for kids without a phone and for people who do not share location.
   *Recommended first step.*
2. **Suggested (learned from places).** The phone notes each time it leaves or arrives at a saved place. After a few
   weeks it says: "You left Home for Work on 5 of the last 6 Mondays, around 8:05. Add it to your plan?" The person
   confirms; nothing is added silently.
3. **Discovered (automatic stays).** The phone finds places on its own (a spot where the phone stays 20+ minutes) and
   proposes names. Most work, most privacy questions; later, if wanted.

### Learning rule (explainable, no machine learning)

Pure Kotlin in `:core:model`, tested on the plain JVM:

- Input: a list of "left place A at time T1 and arrived at place B at T2" for the last N weeks (N = 8).
- Group by weekday and by (from, to). A habit needs at least 3 occurrences in the last 8 weeks.
- Typical time = the median departure time; spread = the middle 50 % range. Confidence = share of those weekdays on
  which it happened.
- Output: `ExpectedTrip(weekday, from, to, leaveAt, arriveAt, confidence)`. Anything under a confidence threshold
  stays a suggestion and is never used for alerts.
- Holidays and travel: a trip not seen for 3 weeks fades out; the "away" mode (already in the app) pauses the person.

Why not machine learning: a handful of data points per weekday does not need it, and "you left at 8:05 on 5 of 6
Mondays" can be shown to the person and corrected, which a model cannot.

## 4. Weather data

| Service | Free for this? | Notes |
|---|---|---|
| **Open-Meteo** | Yes for **non-commercial** use, no key, no account. Official limits: under 10,000 calls a day, 5,000 an hour, 600 a minute. Data under CC BY 4.0, so a credit is shown. Commercial use needs a paid plan. | Hourly rain probability and amount, temperature, wind, UV, weather code. Also an air-quality and a pollen endpoint. Best fit. |
| **MET Norway (api.met.no)** | Free, but every request must carry an identifying `User-Agent` with contact details, and cache headers must be respected. Fair-use rules apply. | Global, good quality; rain *probability* is thinner than Open-Meteo's. A good backup. |
| **Bright Sky (DWD)** | Free, no key | Germany only. |

Recommendation: **Open-Meteo**, with MET Norway as the fallback if the terms ever stop fitting. The app is free, has no ads and
no income, which fits "non-commercial" today; if that ever changes, the paid plan or the fallback has to be chosen.
Terms must be reread before release (checked by search on 13 November 2026, not from inside this session's network,
which does not reach these hosts).

Load: one request per distinct place and day, cached for a few hours. A family needs a few dozen calls a day, far under
the limits. The app never talks to the weather service with a family id, a name or an account.

## 5. What it says (advice rules)

Pure functions in `:core:model`, tested:

- **Rain:** probability and amount in the hour of leaving *and* the hour of arriving ("rain at 8:00 when you leave, dry
  when you arrive"). Levels: none / possible (30 to 60 %) / likely (over 60 %) / heavy (over 4 mm an hour).
- **Cold, frost, ice:** under 3 degrees ("frost on the way"), under 0 degrees at departure for cyclists and walkers.
- **Heat, UV:** over 30 degrees, UV 6 or more at midday.
- **Wind, storm:** gusts over 60 km/h, weather codes for thunderstorm.
- **Comfort:** "jacket", "umbrella", "sunglasses" as small icons, never more than two items.
- **Quiet by default:** nothing is said on a day when nothing matters. No "partly cloudy at 14 degrees" notifications.

The one sentence is built from the worst thing in the window: "Monday 8:00, work: rain 70 %, umbrella".

## 6. Where it appears

No new tab (the bar has six already).

1. **A line in the yellow hero card on Chores:** "Rain at 8:00: umbrella". Tap opens the details.
2. **A notification** the evening before (default 20:00) and, if the plan changes (the forecast moved a lot), in the
   morning, 30 minutes before the usual leaving time. Per phone, off by default until the person sets up a plan.
   Respects quiet hours.
3. **"My week" screen** (opened from the hero line or from Family > This phone): the plan as a small week grid with a
   weather symbol per trip, and the editor for the manual plan and the suggestions.
4. **Family view:** "Today: Leo, school 7:40, rain" for parents, only for people who agreed to share their plan.
5. **Chores that depend on weather** (phase 3): "Hang the laundry" is suggested when the day is dry and sunny;
   "Water the plants" is skipped after rain; "Mow the lawn" waits for a dry day. Shown as a small tag on the chore.
6. **Widget:** one line for the next trip.

## 7. Privacy and Play Store (must be right from the start)

- **Learning stays on the phone.** The place history lives only in that phone's storage. Only the *plan* (weekday, time,
  place name) is ever written to the family's database, and only if the person turns on "Share my plan with the family".
  Kids' plans can be created by a parent.
- **Coordinates to the weather service:** the place's centre, **rounded to 2 decimals (about 1 km)**, no id, no
  account, no family data. Still personal data: it goes into the privacy policy and the Play **Data safety** form
  ("approximate location shared with a third-party service for the weather"). `docs/privacy.html` and
  `docs/play-store/data-safety.md` are updated in the same change.
- **Off by default**, one switch under This phone, and a plan can be deleted at any time ("Forget my routine" removes the
  learned history).
- **Firestore rules and emulator tests** for any new collection (for example `families/{f}/plans/{memberId}`), as the
  project contract requires: a member writes their own plan, parents write kids' plans, everyone in the family may read
  only plans marked shared.
- Background location is already declared on Play; the learning reuses it and adds no new permission.

## 8. Design and build plan

Modules (CLAUDE.md contract: logic in `:core:model`, features never depend on each other):

| Module | New work |
|---|---|
| `:core:model` | `Routine`, `ExpectedTrip`, `learnRoutine(...)`, `WeatherHour`, `TripForecast`, `adviceFor(...)`, `nextTrips(...)` + JVM tests |
| `:core:weather` (new core module, added to `ModuleRules.CORE_DEPENDENCIES`, may depend on `:core:model`) | Open-Meteo client, response parsing, cache (a few hours), rounding of coordinates |
| `:core:data` | `PlanRepository` (the shared plan), schema names |
| `:core:location` | a small local log of leaving / arriving at saved places (this phone only) |
| `:feature:weather` (new) | "My week" screen, plan editor, suggestions, notification worker, settings row |
| `:feature:chores` | the line in the hero card and the weather tag on chores, through a small interface from `:core:weather` |
| `:app` | wires the entry point and the route |

Phases:

| Phase | What | Done when |
|---|---|---|
| W0 | Answers to section 9 (done); the CI screenshots serve as mock-ups | owner looks at them after W4 |
| W1 | Pure model + advice rules + tests (no Android, no network) | `./gradlew :core:model:test` green |
| W2 | `:core:weather`: client, parsing, cache; rules checked against recorded responses | unit tests with saved JSON, no live calls in CI |
| W3 | Manual plan: editor, storage, Firestore rules and emulator tests, the "My week" screen | owner sets a Monday trip and sees its forecast |
| W4 | The hero line, the evening notification, quiet hours, the settings switch | owner gets the notification on a real phone |
| W5 | Learning from places: local log, `learnRoutine`, suggestions ("add to my plan?") | after two weeks of use the suggestions are right |
| W6 | Weather-aware chores, family view, widget line | |
| W7 | Wording EN and Baianês, privacy policy and Data safety update, Play screenshots | |

## 9. Decisions (answered by the owner, 13 November 2026)

1. **Units:** degrees Celsius and km/h by default, **changeable in the settings** (this phone): temperature C or F,
   wind km/h, mph or m/s, rain mm or inches.
2. **Manual plan first**, learning from places after.
3. **Whose routine:** every adult (parents). Kids are not tracked; a parent can add a kid's school run by hand if wanted.
4. **Moments that matter:** work, bike rides, the gym, the weekend.
5. **One morning notification** that sums up the weather for **every important moment of that day**, not a separate
   alert for each.
6. **Approximate coordinates (about 1 km) may be sent to the weather service**, and the privacy policy says so.
7. **The whole trip counts:** the weather at the start, at the end and in between (hour by hour over the trip's length).
8. **Forecast only**, no history statistics.

Ideas (section 10): **yes** to "leave earlier", **yes** to the seasonal pause of habits, **yes** to the family morning
card, **no** to the Sunday weekly outlook, **yes** to the extreme-weather alerts, **yes** to air quality and pollen.
Laundry drying day: yes (it was in the same list as rain-aware chores).

**The little house changes job.** The "Our home" picture at the top of Chores (the pixel house and garden that used to
react to how many chores waited) now shows the **real weather outside**: sun, clouds, rain, snow, storm, fog, night with
lit windows. Next to it: the temperature now and the day's moments ("8:00 work: rain 70 %, umbrella"). The chore-driven
parts (weeds, flowers, "tired garden", the week's points dialog) go away; the weekly points stay on the Family tab.
Tapping the card opens the day in detail.

Where the "outside" weather comes from: the phone's approximate position, else the family's saved place called Home,
else a prompt to name Home on the map.

Still open (decide while building, small): which saved place is "work" and "gym" (each trip points at a saved place, so
the Map tab's places are the list to pick from), and whether bike trips need a "how long" field (default 30 minutes).

## 10. Ideas to explore (a menu, not a promise)

- **"Leave earlier" nudges:** heavy rain or ice means the commute takes longer; "leave 10 minutes earlier" using the
  person's own usual trip duration.
- **Seasonal habits:** the routine changes with school terms and holidays; the app notices ("no school trips for 2 weeks")
  and pauses the alerts.
- **Family dashboard in the morning:** one card: "Today: Ana (work 8:30, dry), Leo (school 7:40, rain), Mia (nursery, dry)".
- **Rain vs bike or walking:** kids who cycle to school get an alert when the road will be wet or icy.
- **Laundry drying score:** "Good drying day until 16:00" (sun, wind, low humidity) from the weather; links to the
  Laundry room in the Rooms tab.
- **Garden:** the Garden room's tasks adapt: watering skipped after rain, frost warning for plants on the balcony.
- **Weekly outlook on Sunday evening:** "Tuesday and Thursday mornings are wet", next to the weekly summary the app
  already sends.
- **Extreme weather:** storm, heat wave, frost: a louder alert, not tied to the routine, for everyone in the family.
- **Air quality and pollen** (Open-Meteo has both): for families with allergies.
- **Sunrise and sunset** for winter commutes ("dark when you leave at 17:00") and for kids walking home.
- **Learning "usual duration"** between places, so the "leave earlier" nudge and the arrival hour are accurate.
- **A weather mood for the home picture:** the pixel house on Chores shows the real weather outside.
- **Plan sharing between households** (grandparents picking the kids up): out of scope, but a natural next step.

## 11. Risks

- **Accuracy:** forecasts at a given hour are good for 1 to 2 days, fair for 3 to 5; the app only alerts within 48 hours and
  shows "check again closer" beyond that.
- **Notification fatigue:** quiet by default, one notification a day at most, easy switch off.
- **Wrong routine:** learned habits can be wrong (a week of holiday); suggestions need a confirmation and expire.
- **Privacy and children's data:** keep learning on the phone, make plans opt-in, document everything in the privacy policy.
- **Service terms:** the free tier is for non-commercial use only; the plan must be revisited if the app ever earns money.
- **Battery:** learning reuses the existing geofence and place events, adding no new location polling.
- **Time zones and travel:** times are stored as local wall-clock times with the place's zone, so a trip abroad does not
  fire alerts at the wrong hour.
