var info = {};
info.Title = window.br?.bookTitle;
const meta = new Map([
['by', 'Author'],
['Isbn', 'ISBN'],
['Language', 'Language'],
['Publisher', 'Publisher'],
['Publication date', 'Published'],
['Contributor', 'Contributor']
]);
const metadata = document.getElementsByClassName('metadata-definition');
for (var i = 0; i < metadata.length; i++) {
    const metaname = metadata[i]?.children[0]?.innerText;
    if (meta.has(metaname)) {
        info[meta.get(metaname)] = metadata[i]?.children[1]?.innerText;
    }
}
info.Producer = 'Element Davv';
info.Creator = 'Timelegend';
info.Created = new Date();
return info;
