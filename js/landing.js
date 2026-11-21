/**
 * Landing: the inventory window in #producto is a working selector.
 * Choosing a property on the left opens its record on the right (price, facts and
 * the clients interested in it). Static sample data, the same as the CRM demo.
 */
const PROPERTIES = {
  'local-centro': {
    kicker: 'Venta · Local · Centro Histórico, Cartagena', title: 'Local en el Centro Histórico', price: '$ 380.000.000',
    facts: ['55 m²', '1 baño', 'Asesora: Valentina Ríos'],
    leads: [['Paula Ruiz', 'WhatsApp · visita el sábado a las 10:00 a. m.', 'neg', 'Visita'], ['Tomás Ortiz', 'Portal web · pidió los planos del local', 'info', 'Contactado']],
  },
  'apto-envigado': {
    kicker: 'Arriendo · Apartamento · Envigado, Antioquia', title: 'Apartamento para Estrenar cerca al Metro', price: '$ 1.900.000 / mes',
    facts: ['68 m²', '2 habitaciones', '2 baños', '1 parqueadero', 'Asesora: Laura Mendoza'],
    leads: [['Sofía Herrera', 'Portal web · pregunta si admiten mascotas', 'info', 'Nuevo'], ['Daniel Restrepo', 'Llamada · quiere mudarse en enero', 'info', 'Contactado']],
  },
  'casa-campestre': {
    kicker: 'Venta · Casa · Llanogrande, Rionegro', title: 'Casa Campestre con Jardín y Chimenea', price: '$ 890.000.000',
    facts: ['280 m²', '4 habitaciones', '4 baños', '2 parqueaderos', 'Asesor: Andrés Rojas'],
    leads: [['Felipe Arango', 'Referido · ofertó $ 850 millones', 'neg', 'Negociación'], ['Lucía Vargas', 'Portal web · pidió el certificado de tradición', 'info', 'Contactado'], ['Mateo Torres', 'Llamada · visita el jueves a las 4:00 p. m.', 'neg', 'Visita'], ['Camila Suárez', 'WhatsApp · registrado hace un momento', 'info', 'Nuevo']],
  },
  penthouse: {
    kicker: 'Venta · Apartamento · Rosales, Bogotá', title: 'Penthouse Dúplex con Terraza Privada', price: '$ 1.350.000.000',
    facts: ['210 m²', '3 habitaciones', '4 baños', '3 parqueaderos', 'Asesora: Valentina Ríos'],
    leads: [['Mariana López', 'Portal web · escrituración firmada en notaría', 'ok', 'Negocio cerrado']],
  },
  oficina: {
    kicker: 'Arriendo · Oficina · Santa Bárbara, Bogotá', title: 'Oficina Ejecutiva en Torre Empresarial', price: '$ 4.800.000 / mes',
    facts: ['75 m²', '2 baños', '2 parqueaderos', 'Asesor: Andrés Rojas'],
    leads: [['Diego Martínez', 'Visita a la oficina · la necesita desde enero', 'info', 'Contactado']],
  },
  'casa-cali': {
    kicker: 'Venta · Casa · Ciudad Jardín, Cali', title: 'Casa Familiar en Conjunto Cerrado', price: '$ 620.000.000',
    facts: ['190 m²', '4 habitaciones', '3 baños', '2 parqueaderos', 'Asesora: Valentina Ríos'],
    leads: [['Ana Gómez', 'WhatsApp · visita el sábado a las 10:00 a. m.', 'neg', 'Visita'], ['Julián Castro', 'Referido · ofertó $ 600 millones', 'neg', 'Negociación']],
  },
  'local-poblado': {
    kicker: 'Arriendo · Local · El Poblado, Medellín', title: 'Local Comercial sobre Avenida Principal', price: '$ 6.500.000 / mes',
    facts: ['90 m²', '1 baño', 'Asesor: Andrés Rojas'],
    leads: [['Jorge Castaño', 'Llamada · registrado ayer', 'info', 'Nuevo']],
  },
};

const EASE_OUT = 'cubic-bezier(0.23, 1, 0.32, 1)';
const reducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const initials = (name) => name.split(' ').map((word) => word[0]).slice(0, 2).join('');

function leadMarkup([name, note, tone, stage]) {
  return `<div class="af-lead"><i>${initials(name)}</i><span><b>${name}</b><small>${note}</small></span><em class="st st--${tone}">${stage}</em></div>`;
}

function openMarkup(p) {
  return `
    <div class="af-open__top">
      <div><p class="af-kicker">${p.kicker}</p><p class="af-title">${p.title}</p></div>
      <p class="af-price">${p.price}</p>
    </div>
    <p class="af-facts">${p.facts.map((fact) => `<span>${fact}</span>`).join('')}</p>
    <p class="af-sub">Clientes interesados · ${p.leads.length}</p>
    <div class="af-leads">${p.leads.map(leadMarkup).join('')}</div>`;
}

/** Swaps the record with a short rise-and-fade (opacity only under reduced motion). */
function showProperty(frame, row) {
  const property = PROPERTIES[row.dataset.prop];
  const pane = frame.querySelector('.af-open');
  if (!property || row.classList.contains('is-on')) return;
  frame.querySelectorAll('.af-row').forEach((item) => {
    const on = item === row;
    item.classList.toggle('is-on', on);
    item.setAttribute('aria-pressed', String(on));
  });
  pane.innerHTML = openMarkup(property);
  const from = reducedMotion() ? { opacity: 0 } : { opacity: 0, transform: 'translateY(6px)' };
  const to = reducedMotion() ? { opacity: 1 } : { opacity: 1, transform: 'none' };
  pane.animate([from, to], { duration: 220, easing: EASE_OUT });
}

document.querySelectorAll('[data-app-frame]').forEach((frame) => {
  frame.addEventListener('click', (event) => {
    const row = event.target.closest('.af-row');
    if (row) showProperty(frame, row);
  });
});
