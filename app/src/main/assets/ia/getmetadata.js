var info = {};
info.Title = window.br?.bookTitle;
const meta = new Map([
['by', 'Authors'],
['Publication date', 'Published'],
['Publisher', 'Publisher'],
['Contributor', 'Contributor'],
['Language', 'Languages'],
['Isbn', 'ISBN']
]);
const metadata = document.getElementsByClassName('metadata-definition');
for (var i = 0; i < metadata.length; i++) {
    const metaname = metadata[i].children[0]?.innerText;
    if (meta.has(metaname)) {
        info[meta.get(metaname)] = metadata[i].children[1]?.innerText;
    }
}
info.Producer = 'Element Davv';
info.Creator = 'Javier Santo Domingo';
info.CreationDate = new Date().toISOString();
return info;
