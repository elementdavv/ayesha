var s = document.getElementById('iadscaleid');
if (!s) return false;
const factors = window.br?.reductionFactors;
const star = "★★★★";
var n = 0;
var lastscale = 1;
factors.forEach(f => {
    const scal = Math.pow(2, Math.floor(Math.log2(Math.max(1, f.reduce))));
    if (scal > lastscale) {
        if (n < 3) {
            var o = document.createElement('option');
            o.value = scal;
            o.innerText = star.substring(++n);
            s.appendChild(o);
        }
        lastscale = scal;
    }
});
return true;
