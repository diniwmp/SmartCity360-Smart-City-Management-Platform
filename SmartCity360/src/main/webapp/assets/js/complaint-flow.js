document.addEventListener('DOMContentLoaded', function () {

    const radios = document.querySelectorAll('.sev-radio');
    const routeNote = document.getElementById('routeNote');

    function refreshRoute() {
        radios.forEach(function (radio) {
            radio.closest('.sev-option').classList.toggle('checked', radio.checked);
        });
        if (!routeNote) return;
        const selected = document.querySelector('.sev-radio:checked');
        routeNote.textContent = '';
        if (!selected) {
            routeNote.textContent = 'Choose a severity to see who will handle it first.';
            return;
        }
        routeNote.append('First handled by: ');
        const strong = document.createElement('strong');
        strong.textContent = selected.dataset.route;
        routeNote.append(strong);
    }
    radios.forEach(function (radio) { radio.addEventListener('change', refreshRoute); });
    refreshRoute();

    const description = document.getElementById('description');
    const counter = document.getElementById('descCount');
    if (description && counter) {
        const update = function () { counter.textContent = description.value.length; };
        description.addEventListener('input', update);
        update();
    }

    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!confirm(form.dataset.confirm)) e.preventDefault();
        });
    });

    if (location.hash === '#detail') {
        const panel = document.getElementById('detail');
        if (panel) panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
});