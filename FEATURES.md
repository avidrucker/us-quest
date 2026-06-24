# Adventures in Us — Features (BDD scenarios)

Scenarios are written Given/When/Then and mirrored as `testing` blocks in the
test suite. Checked boxes are implemented and covered by passing tests.

## Domain — building & playing adventures (`adventure.domain`) ✅

- [x] **Create** — Given a title, a new adventure has that title, a generated id,
  no passages, and no start.
- [x] **First passage is the start** — Given an adventure with no start, when the
  first passage is added, then it becomes the start.
- [x] **Add more passages** — When further passages are added, the start is
  unchanged and each passage is retrievable by id (missing ids return nil).
- [x] **Branching** — Given a choice is added to a passage, it is appended there;
  when the choice is followed, its target passage is returned.
- [x] **Endings** — A passage with no choices is an ending; one with choices is not.
- [x] **Editing** — Passage text can be changed; the start can be reassigned; a
  choice can be retargeted; removing a passage prunes choices that pointed to it
  and clears the start if it was the start.
- [x] **Validation** — `validate` reports `:problem/no-start`,
  `:problem/dangling-start`, `:problem/dangling-choice`, and `:problem/unreachable`;
  a reachable adventure with an ending is `valid?`.

## Player (`adventure` re-frame views) — Milestone 3

- [ ] Starting a playthrough shows the start passage.
- [ ] Choosing an option appends the next passage below (append-and-scroll) and
  records the path.
- [ ] Reaching an ending shows the payoff and a "start over" action.
- [ ] "Back" returns to the previous passage in the path.

## Library & persistence — Milestone 4

- [ ] Saved adventures survive a page reload (localStorage).
- [ ] The library lists saved adventures with Play / Edit / New.

## Authoring — Milestone 5

- [ ] A form-based editor can add/edit passages and choices and set the start.
- [ ] The editor surfaces validation problems live and offers an inline preview.

## Sharing — Milestone 6

- [ ] An adventure can be encoded into a share link and decoded back identically
  (round-trip = identity).
- [ ] Opening a share link imports the adventure for playing.
