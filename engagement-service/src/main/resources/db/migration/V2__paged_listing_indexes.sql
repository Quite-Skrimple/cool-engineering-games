-- Listings are paged newest-first per game, so these indexes let the database read just one page.
create index idx_comments_game_created on comments (game_id, created_at);
create index idx_reactions_game_created on reactions (game_id, created_at);
