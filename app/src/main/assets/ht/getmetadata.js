var info = {};
var meta = window.manifest?.metadata;
if (meta?.title) info.Title = meta.title;
if (meta?.author) info.Authors = meta.author;
if (meta?.publisher) info.Publisher = meta.publisher;
if (meta?.publicationDate) info.Published = meta.publicationDate;
info.Producer = 'Element Davv';
info.Creator = 'Javier Santo Domingo';
info.CreationDate = new Date().toISOString();
return info;
